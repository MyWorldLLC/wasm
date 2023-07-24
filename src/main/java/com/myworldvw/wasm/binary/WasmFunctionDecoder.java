package com.myworldvw.wasm.binary;

import java.nio.ByteBuffer;

public class WasmFunctionDecoder {

    protected final ByteBuffer code;

    public WasmFunctionDecoder(Code code){
        this.code = ByteBuffer.wrap(code.binaryFunction());
    }

}
