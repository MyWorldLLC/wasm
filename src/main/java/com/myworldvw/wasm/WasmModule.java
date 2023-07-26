package com.myworldvw.wasm;

public abstract class WasmModule {

    protected final String name;

    public WasmModule(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

}
