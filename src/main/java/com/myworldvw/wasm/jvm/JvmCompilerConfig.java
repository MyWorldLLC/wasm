package com.myworldvw.wasm.jvm;

public class JvmCompilerConfig {

    protected int trapFlags;

    protected final WasmClassLoader loader;

    public JvmCompilerConfig(){
        loader = new WasmClassLoader(JvmCompilerConfig.class.getClassLoader());
    }

    public WasmClassLoader getModuleLoader(){
        return loader;
    }

}
