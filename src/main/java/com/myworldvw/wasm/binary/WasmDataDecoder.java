package com.myworldvw.wasm.binary;

import com.myworldvw.wasm.jvm.JvmCodeVisitor;
import com.myworldvw.wasm.util.Leb128;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WasmDataDecoder {

    protected final ByteBuffer code;

    public WasmDataDecoder(byte[] globalSection){
        code = ByteBuffer.wrap(globalSection);
        code.order(ByteOrder.LITTLE_ENDIAN);
    }

    public int decodeDataCount(){
        return decodeU32();
    }

    public int decodeMemoryId(){
        return decodeU32();
    }

    public void decodeOffsetExpr(JvmCodeVisitor visitor){
        var decoder = new WasmFunctionDecoder(code);
        decoder.decodeExpression(visitor);
    }

    public byte[] decodeData(){
        var size = decodeU32();
        var data = new byte[size];
        System.arraycopy(code.array(), code.position(), data, 0, size);
        return data;
    }

    public int decodeU32() throws WasmFormatException {
        return (int) Leb128.decodeUnsigned(code);
    }

}
