package com.myworldvw.wasm;

import com.myworldvw.wasm.binary.Import;
import com.myworldvw.wasm.binary.WasmBinaryModule;
import com.myworldvw.wasm.binary.WasmModuleDecoder;
import com.myworldvw.wasm.globals.Global;
import com.myworldvw.wasm.jvm.JvmCompiler;
import com.myworldvw.wasm.jvm.JvmCompilerConfig;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

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

    public Optional<WasmBinaryModule> findBinary(String moduleName){
        return modules.stream().filter(m -> m.getName().equals(moduleName)).findFirst();
    }

    public WasmModule findInstance(String moduleName){
        return instantiatedModules.stream().filter(m -> m.getName().equals(moduleName)).findFirst().get();
    }

    public WasmModule instantiate(String name) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, MissingImportException {
        return instantiate(name, new Imports());
    }

    public WasmModule instantiate(String name, Imports imports) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, MissingImportException {
        var requiredImports = findBinary(name).get().getImportSection();
        var instance = compile(name).getConstructor(String.class, Import[].class).newInstance(name, requiredImports);

        if(requiredImports != null){
            for(var required : requiredImports){
                var field = fieldForImport(instance, required);
                switch (required.descriptor().type()){
                    case TYPE_ID -> field.set(instance, imports.getFunction(required.module(), required.name()));
                    case MEMORY_TYPE -> field.set(instance, imports.getMemory(required.module(), required.name()));
                    case TABLE_TYPE -> field.set(instance, imports.getTable(required.module(), required.name()));
                    case GLOBAL_TYPE -> field.set(instance, imports.getGlobal(required.module(), required.name()));
                }
            }
        }

        instance.initialize();
        instance.initializationComplete();

        instantiatedModules.add(instance);
        return instance;
    }

    protected Field fieldForImport(WasmModule instance, Import i){
        return Arrays.stream(instance.getClass().getDeclaredFields())
                .filter(f -> {
                    var wi = f.getDeclaredAnnotation(WasmImport.class);
                    return wi != null && wi.module().equals(i.module()) && wi.name().equals(i.name());
                })
                .filter(f -> switch (i.descriptor().type()){
                    case TYPE_ID -> f.getType().equals(MethodHandle.class);
                    case TABLE_TYPE -> f.getType().equals(Table.class);
                    case MEMORY_TYPE -> f.getType().equals(Memory.class);
                    case GLOBAL_TYPE -> Global.class.isAssignableFrom(f.getType());
                })
                .findFirst()
                .orElse(null);
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

    public Optional<Global<?>> getExportedGlobal(String moduleName, String globalName){
        var module = findInstance(moduleName);

        return Arrays.stream(module.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(WasmExport.class))
                .filter(f -> f.getName().equals(globalName))
                .findFirst()
                .map(f -> {
                    try {
                        return (Global<?>) f.get(module);
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

    public void instantiateAll() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, MissingImportException {
        for(var module : modules){
            instantiate(module.getName());
        }
    }

    public static void main(String[] args){
        var wasmVm = new WasmContext();


    }

}
