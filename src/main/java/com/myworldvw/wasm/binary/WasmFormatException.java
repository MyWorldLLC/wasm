package com.myworldvw.wasm.binary;

public class WasmFormatException extends RuntimeException {

    public WasmFormatException(String msg){
        super(msg);
    }

    public WasmFormatException(byte value, String target){
        super("0x%02X is not a valid %s".formatted(value, target));
    }

}
