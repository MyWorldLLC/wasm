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

import com.myworldvw.wasm.util.Leb128;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Stack;

import static com.myworldvw.wasm.binary.WasmOpcodes.*;
import static com.myworldvw.wasm.binary.WasmOpcodes.F64_MAX;

public class WasmFunctionDecoder {

    protected final ByteBuffer code;
    protected final FunctionType type;
    protected final Stack<Optional<ValueType>> blockTypes;

    public WasmFunctionDecoder(Code code, FunctionType type){
        this.code = ByteBuffer.wrap(code.binaryFunction());
        this.code.order(ByteOrder.LITTLE_ENDIAN);
        this.type = type;
        blockTypes = new Stack<>();
    }

    public WasmFunctionDecoder(ByteBuffer code){
        this.code = code;
        this.type = null;
        this.blockTypes = new Stack<>();
    }

    public void decode(CodeVisitor visitor) throws WasmFormatException {
        visitor.visitFunction(type);
        visitor.visitLocals(decodeLocalVec());
        decodeExpression(visitor);
    }

    public void decodeExpression(CodeVisitor visitor) throws WasmFormatException {

        while(code.hasRemaining()){
            var opcode = code.get();
            switch (opcode){
                case UNREACHABLE, NOP, RETURN -> visitor.visitCtrl(opcode);
                case END -> {
                    if(blockTypes.isEmpty() && type != null){
                        visitor.exitFunction();
                    }else if(!blockTypes.empty()){
                        visitor.exitBlock();
                        blockTypes.pop();
                    }
                }
                case BLOCK, LOOP, IF -> {
                    var blockType = decodeBlockType();
                    blockTypes.push(blockType);
                    visitor.visitBlock(opcode, blockType);
                }
                case ELSE -> {
                    visitor.visitBlock(opcode, blockTypes.peek());
                }
                case BR, BR_IF -> {
                    var target = decodeU32();
                    visitor.visitBranch(opcode, target);
                }
                case BR_TABLE -> {
                    var labels = decodeLabelVec();
                    var defaultLabel = decodeU32();
                    visitor.visitTableBranch(opcode, labels, defaultLabel);
                }
                case CALL, CALL_INDIRECT -> {
                    visitor.visitCall(opcode, decodeU32());
                    if(opcode == CALL_INDIRECT){
                        code.get(); // Drop the trailing 0x00
                    }
                }
                case DROP, SELECT -> {
                    visitor.visitParametric(opcode);
                }
                case LOCAL_GET, LOCAL_SET, LOCAL_TEE,
                        GLOBAL_GET, GLOBAL_SET -> {
                    visitor.visitVar(opcode, decodeU32());
                }
                case I32_LOAD,
                        I64_LOAD,
                        F32_LOAD,
                        F64_LOAD,
                        I32_LOAD_8_S,
                        I32_LOAD_8_U,
                        I32_LOAD_16_S,
                        I32_LOAD_16_U,
                        I64_LOAD_8_S,
                        I64_LOAD_8_U,
                        I64_LOAD_16_S,
                        I64_LOAD_16_U,
                        I64_LOAD_32_S,
                        I64_LOAD_32_U,
                        I32_STORE,
                        I64_STORE,
                        F32_STORE,
                        F64_STORE,
                        I32_STORE_8,
                        I32_STORE_16,
                        I64_STORE_8,
                        I64_STORE_16,
                        I64_STORE_32-> {
                    visitor.visitMemory(opcode, decodeU32(), decodeU32());
                }
                case MEMORY_SIZE, MEMORY_GROW -> {
                    visitor.visitMemory(opcode, -1, -1);
                    code.get(); // Drop trailing 0x00
                }
                case I32_CONST -> {
                    visitor.visitConst(opcode, decodeI32());
                }
                case I64_CONST -> {
                    visitor.visitConst(opcode, decodeI64());
                }
                case F32_CONST -> {
                    visitor.visitConst(opcode, Float.floatToIntBits(decodeF32()));
                }
                case F64_CONST -> {
                    visitor.visitConst(opcode, Double.doubleToLongBits(decodeF64()));
                }
                case I32_EQZ,
                        I32_EQ,
                        I32_NE,
                        I32_LT_S,
                        I32_LT_U,
                        I32_GT_S,
                        I32_GT_U,
                        I32_LE_S,
                        I32_LE_U,
                        I32_GE_S,
                        I32_GE_U,

                        I64_EQZ,
                        I64_EQ,
                        I64_NE,
                        I64_LT_S,
                        I64_LT_U,
                        I64_GT_S,
                        I64_GT_U,
                        I64_LE_S,
                        I64_LE_U,
                        I64_GE_S,
                        I64_GE_U,

                        F32_EQ,
                        F32_NE,
                        F32_LT,
                        F32_GT,
                        F32_LE,
                        F32_GE,

                        F64_EQ,
                        F64_NE,
                        F64_LT,
                        F64_GT,
                        F64_LE,
                        F64_GE,

                        I32_CLZ,
                        I32_CTZ,
                        I32_POPCNT,
                        I32_ADD,
                        I32_SUB,
                        I32_MUL,
                        I32_DIV_S,
                        I32_DIV_U,
                        I32_REM_S,
                        I32_REM_U,
                        I32_AND,
                        I32_OR,
                        I32_XOR,
                        I32_SHL,
                        I32_SHR_S,
                        I32_SHR_U,
                        I32_ROTL,
                        I32_ROTR,

                        I64_CLZ,
                        I64_CTZ,
                        I64_POPCNT,
                        I64_ADD,
                        I64_SUB,
                        I64_MUL,
                        I64_DIV_S,
                        I64_DIV_U,
                        I64_REM_S,
                        I64_REM_U,
                        I64_AND,
                        I64_OR,
                        I64_XOR,
                        I64_SHL,
                        I64_SHR_S,
                        I64_SHR_U,
                        I64_ROTL,
                        I64_ROTR,

                        F32_ABS,
                        F32_NEG,
                        F32_CEIL,
                        F32_FLOOR,
                        F32_TRUNC,
                        F32_NEAREST,
                        F32_SQRT,
                        F32_ADD,
                        F32_SUB,
                        F32_MUL,
                        F32_DIV,
                        F32_MIN,
                        F32_MAX,
                        F32_COPYSIGN,

                        F64_ABS,
                        F64_NEG,
                        F64_CEIL,
                        F64_FLOOR,
                        F64_TRUNC,
                        F64_NEAREST,
                        F64_SQRT,
                        F64_ADD,
                        F64_SUB,
                        F64_MUL,
                        F64_DIV,
                        F64_MIN,
                        F64_MAX,
                        F64_COPYSIGN,

                        I32_WRAP_I64,
                        I32_TRUNC_F32_S,
                        I32_TRUNC_F32_U,
                        I32_TRUNC_F64_S,
                        I32_TRUNC_F64_U,
                        I64_EXTEND_I32_S,
                        I64_EXTEND_I32_U,
                        I64_TRUNC_F32_S,
                        I64_TRUNC_F32_U,
                        I64_TRUNC_F64_S,
                        I64_TRUNC_F64_U,
                        F32_CONVERT_I32_S,
                        F32_CONVERT_I32_U,
                        F32_CONVERT_I64_S,
                        F32_CONVERT_I64_U,
                        F32_DEMOTE_F64,
                        F64_CONVERT_I32_S,
                        F64_CONVERT_I32_U,
                        F64_CONVERT_I64_S,
                        F64_CONVERT_I64_U,
                        F64_PROMOTE_F32,
                        I32_REINTERPRET_F32,
                        I64_REINTERPRET_F64,
                        F32_REINTERPRET_I32,
                        F64_REINTERPRET_I64
                        -> {
                    visitor.visitNumeric(opcode);
                }
                default -> throw new WasmFormatException(opcode, "opcode");
            }
        }
    }

    protected int decodeI32() throws WasmFormatException {
        return (int) Leb128.decodeSigned(code, 32);
    }

    protected long decodeI64() throws WasmFormatException{
        return Leb128.decodeSigned(code, 64);
    }

    protected int decodeU32() throws WasmFormatException {
        return (int) Leb128.decodeUnsigned(code);
    }

    protected long decodeU64() throws WasmFormatException{
        return Leb128.decodeUnsigned(code);
    }

    protected float decodeF32() throws WasmFormatException {
        return code.getFloat();
    }

    protected double decodeF64() throws WasmFormatException {
        return code.getDouble();
    }

    protected Optional<ValueType> decodeBlockType() throws WasmFormatException {
        var value = code.get();
        return switch (value){
            case 0x40 -> Optional.empty();
            default -> Optional.of(decodeValType(value));
        };
    }

    protected ValueType decodeValType(byte value) throws WasmFormatException {
        return switch (value){
            case 0x7F -> ValueType.I32;
            case 0x7E -> ValueType.I64;
            case 0x7D -> ValueType.F32;
            case 0x7C -> ValueType.F64;
            default -> throw new WasmFormatException(value, "value type");
        };
    }

    protected int[] decodeLabelVec() throws WasmFormatException {
        var length = decodeU32();
        var array = new int[length];
        for(int i = 0; i < length; i++){
            array[i] = decodeU32();
        }
        return array;
    }

    protected ValueType[] decodeLocalVec() throws WasmFormatException {
        var length = decodeU32();
        var locals = new ArrayList<ValueType>();
        for(int i = 0; i < length; i++){
            var n = decodeU32();
            var type = decodeValType(code.get());
            for(int j = 0; j < n; j++){
                locals.add(type);
            }
        }
        return locals.toArray(ValueType[]::new);
    }

}
