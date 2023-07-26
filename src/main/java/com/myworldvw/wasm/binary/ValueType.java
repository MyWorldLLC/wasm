package com.myworldvw.wasm.binary;

public enum ValueType {
    I32(32), I64(64), F32(32), F64(64);
    final int bitSize;

    ValueType(int bits){
        bitSize = bits;
    }

    public int bitSize(){
        return bitSize;
    }
}
