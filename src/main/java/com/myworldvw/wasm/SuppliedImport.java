package com.myworldvw.wasm;

import java.lang.invoke.MethodHandle;

public record SuppliedImport(String module, String name, Object payload) {

    public boolean isMemory(){
        return payload instanceof Memory;
    }

    public boolean isTable(){
        return payload instanceof Table;
    }

    public boolean isFunction(){
        return payload instanceof MethodHandle;
    }

    public boolean isGlobal(){
        return false; // TODO
    }

}
