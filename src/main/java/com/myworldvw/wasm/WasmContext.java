package com.myworldvw.wasm;

import com.myworldvw.wasm.binary.WasmBinaryModule;

import java.util.ArrayList;
import java.util.List;

public class WasmContext {

    protected final List<WasmBinaryModule> modules;

    public WasmContext(){
        modules = new ArrayList<>();
    }

    public static void main(String[] args){
        var wasmVm = new WasmContext();


    }

}
