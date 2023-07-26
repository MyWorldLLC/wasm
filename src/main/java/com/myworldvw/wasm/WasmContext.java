package com.myworldvw.wasm;

import com.myworldvw.wasm.binary.WasmBinaryModule;
import com.myworldvw.wasm.binary.WasmModuleDecoder;
import com.myworldvw.wasm.jvm.JvmCompiler;
import com.myworldvw.wasm.jvm.JvmCompilerConfig;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class WasmContext {

    protected final List<WasmBinaryModule> modules;
    protected final List<Class<? extends WasmModule>> compiled;
    protected final List<WasmModule> instantiatedModules;

    public WasmContext(){
        modules = new ArrayList<>();
        compiled = new ArrayList<>();
        instantiatedModules = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends WasmModule> compile(String name){
        return compile(modules.stream().filter(m -> m.getName().equals(name)).findFirst().get());
    }

    @SuppressWarnings("unchecked")
    public Class<? extends WasmModule> compile(WasmBinaryModule module){
        // TODO - check for duplicates before compiling
        var compiler = new JvmCompiler(new JvmCompilerConfig());
        var bytes = compiler.compile(module);
        var cls = (Class<? extends WasmModule>) compiler.getLoader().defineModuleClass(module.getName(), bytes);
        compiled.add(cls);
        return cls;
    }

    public WasmModule findInstance(String moduleName){
        return instantiatedModules.stream().filter(m -> m.getName().equals(moduleName)).findFirst().get();
    }

    public WasmModule instantiate(String name) throws InstantiationException, IllegalAccessException {
        var instance = compile(name).newInstance(); // TODO
        instantiatedModules.add(instance);
        return instance;
    }

    public Optional<MethodHandle> getExportedFunction(String moduleName, String functionName){

        var module = findInstance(moduleName);

        return Arrays.stream(module.getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(WasmExport.class))
                .filter(m -> m.getName().equals(functionName))
                .findFirst()
                .map(m -> {
                    try {
                        return MethodHandles.lookup().unreflect(m).bindTo(module);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e); // TODO
                    }
                });
    }

    public void loadBinary(String name, InputStream is) throws IOException {
        loadBinary(name, is.readAllBytes());
    }

    public void loadBinary(String name, byte[] bytes){
        var decoder = new WasmModuleDecoder(bytes);
        modules.add(decoder.decodeModule(name));
    }

    public void compileAll(){
        for(var module : modules){
            compile(module.getName());
        }
    }

    public void instantiateAll() throws InstantiationException, IllegalAccessException {
        for(var module : modules){
            instantiate(module.getName());
        }
    }

    public static void main(String[] args){
        var wasmVm = new WasmContext();


    }

}
