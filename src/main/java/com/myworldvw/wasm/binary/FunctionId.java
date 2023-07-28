package com.myworldvw.wasm.binary;

public record FunctionId(int id, boolean imported) {
    public FunctionId(int id){
        this(id, false);
    }

    public boolean isLocal(){
        return !isImported();
    }

    public boolean isImported(){
        return imported;
    }
}
