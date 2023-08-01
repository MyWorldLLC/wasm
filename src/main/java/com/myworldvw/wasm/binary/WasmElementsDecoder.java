package com.myworldvw.wasm.binary;

import com.myworldvw.wasm.jvm.JvmCodeVisitor;
import com.myworldvw.wasm.util.Leb128;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WasmElementsDecoder {

    protected final ByteBuffer code;

    protected int elementCount;

    public WasmElementsDecoder(byte[] elementsSection){
        code = ByteBuffer.wrap(elementsSection);
        code.order(ByteOrder.LITTLE_ENDIAN);
    }

    public int decodeElementCount() throws WasmFormatException {
        elementCount = decodeU32();
        return elementCount;
    }

    public void decodeOffsetExpr(JvmCodeVisitor visitor){
        var decoder = new WasmFunctionDecoder(code);
        decoder.decodeExpression(visitor);
    }

    public int[] decodeIds(){
        var size = decodeU32();
        var ids = new int[size];
        for(int i = 0; i < size; i++){
            ids[i] = decodeU32();
        }
        return ids;
    }

    public int decodeU32() throws WasmFormatException {
        return (int) Leb128.decodeUnsigned(code);
    }

    protected byte peek(){
        var pos = code.position();
        var value = code.get();
        code.position(pos);
        return value;
    }

}
