package com.myworldvw.wasm.binary;

import com.myworldvw.wasm.jvm.JvmCodeVisitor;
import com.myworldvw.wasm.util.Leb128;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WasmElementsDecoder {

    protected final ByteBuffer wasm;

    protected int elementCount;

    public WasmElementsDecoder(byte[] elementsSection){
        wasm = ByteBuffer.wrap(elementsSection);
        wasm.order(ByteOrder.LITTLE_ENDIAN);
    }

    public int decodeElementCount() throws WasmFormatException {
        elementCount = decodeU32();
        return elementCount;
    }

    public void decodeOffsetExpr(JvmCodeVisitor visitor){
        // TODO
    }

    public int decodeU32() throws WasmFormatException {
        return (int) Leb128.decodeUnsigned(wasm);
    }

}
