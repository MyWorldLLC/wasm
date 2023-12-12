/*
 * Copyright 2023. MyWorld, LLC
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.myworldvw.wasm.binary;

import com.myworldvw.wasm.globals.Mutability;
import com.myworldvw.wasm.jvm.JvmCodeVisitor;
import com.myworldvw.wasm.util.Leb128;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WasmGlobalDecoder {

    protected final ByteBuffer code;
    protected int globalCount;

    public WasmGlobalDecoder(byte[] bytes){
        this(ByteBuffer.wrap(bytes));
    }

    public WasmGlobalDecoder(ByteBuffer code){
        this.code = code;
        code.order(ByteOrder.LITTLE_ENDIAN);
    }

    public int decodeGlobalCount() throws WasmFormatException {
        globalCount = decodeU32();
        return globalCount;
    }

    public ValueType decodeValType() throws WasmFormatException {
        var value = code.get();
        return switch (value){
            case 0x7F -> ValueType.I32;
            case 0x7E -> ValueType.I64;
            case 0x7D -> ValueType.F32;
            case 0x7C -> ValueType.F64;
            default -> throw new WasmFormatException(value, "value type");
        };
    }

    public Mutability decodeMutability() throws WasmFormatException {
        var value = code.get();
        return switch (value){
            case 0x00 -> Mutability.CONST;
            case 0x01 -> Mutability.VAR;
            default -> throw new WasmFormatException(value, "mutability flag");
        };
    }

    public GlobalType decodeGlobalType() throws WasmFormatException {
        return new GlobalType(decodeValType(), decodeMutability());
    }

    public boolean decodeInitializer(JvmCodeVisitor visitor){
        if(code.hasRemaining() && peek() == WasmOpcodes.END){
            return false;
        }

        var decoder = new WasmFunctionDecoder(code);
        decoder.decodeExpression(visitor);
        return true;
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
