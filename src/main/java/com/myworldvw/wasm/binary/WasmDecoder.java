package com.myworldvw.wasm.binary;

import com.myworldvw.wasm.util.Leb128;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

public class WasmDecoder {

    public static final byte[] MAGIC = new byte[]{0x00, 0x61, 0x73, 0x6D};

    protected final ByteBuffer wasm;

    public WasmDecoder(ByteBuffer wasm){
        this.wasm = wasm;
        wasm.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void begin() throws WasmFormatException {
        wasm.rewind();

        for(int i = 0; i < MAGIC.length; i++){
            if(wasm.get() != MAGIC[i]){
                throw new WasmFormatException("Bad magic number. Is this a wasm module?");
            }
        }

        var version = wasm.getInt();
        if(version != 1){
            throw new WasmFormatException("Unsupported wasm version %d. Only version 1.0 is currently supported.".formatted(version));
        }
    }

    public int decodeI32() throws WasmFormatException {
        return (int) Leb128.decodeSigned(wasm, 32);
    }

    public long decodeI64() throws WasmFormatException{
        return Leb128.decodeSigned(wasm, 64);
    }

    public int decodeU32() throws WasmFormatException {
        return (int) Leb128.decodeUnsigned(wasm);
    }

    public long decodeU64() throws WasmFormatException{
        return Leb128.decodeUnsigned(wasm);
    }

    public float decodeF32() throws WasmFormatException {
        return wasm.getFloat();
    }

    public double decodeF64() throws WasmFormatException {
        return wasm.getDouble();
    }

    public ValueType decodeValType(byte value) throws WasmFormatException {
        return switch (value){
            case 0x7F -> ValueType.I32;
            case 0x7E -> ValueType.I64;
            case 0x7D -> ValueType.F32;
            case 0x7C -> ValueType.F64;
            default -> throw new WasmFormatException(value, "value type");
        };
    }

    public ValueType decodeValType() throws WasmFormatException {
        return decodeValType(wasm.get());
    }

    public Optional<ValueType> decodeBlockType() throws WasmFormatException {
        var value = wasm.get();
        return switch (value){
            case 0x40 -> Optional.empty();
            default -> Optional.of(decodeValType(value));
        };
    }

    public Limits decodeLimits() throws WasmFormatException {
        var value = wasm.get();
        return switch (value){
            case 0x00 -> new Limits(decodeU32());
            case 0x01 -> new Limits(decodeU32(), decodeU32());
            default -> throw new WasmFormatException(value, "limit flag");
        };
    }

    public TableType decodeTableType() throws WasmFormatException {
        var value = wasm.get();
        return switch (value){
            case 0x70 -> new TableType(ElementType.FUNC_REF, decodeLimits());
            default -> throw new WasmFormatException(value, "table type");
        };
    }

    public Mutability decodeMutability() throws WasmFormatException {
        var value = wasm.get();
        return switch (value){
            case 0x00 -> Mutability.CONST;
            case 0x01 -> Mutability.VAR;
            default -> throw new WasmFormatException(value, "mutability flag");
        };
    }

    public GlobalType decodeGlobalType() throws WasmFormatException {
        return new GlobalType(decodeValType(), decodeMutability());
    }
}
