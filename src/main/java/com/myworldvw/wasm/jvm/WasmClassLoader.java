package com.myworldvw.wasm.jvm;

public class WasmClassLoader extends ClassLoader {

    public WasmClassLoader(ClassLoader parent){
        super(parent);
    }

    public Class<?> defineModuleClass(String name, byte[] bytecode){
        return defineClass(name, bytecode, 0, bytecode.length);
    }

}
