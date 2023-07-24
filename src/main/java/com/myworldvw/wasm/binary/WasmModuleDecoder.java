package com.myworldvw.wasm.binary;

import com.myworldvw.wasm.util.Leb128;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * This decoder assumes (as per https://webassembly.github.io/spec/core/appendix/implementation.html)
 * that all table count limits are 2^32 - 1 (the maximum value of a signed integer), and that locals
 * are limited to the maximum number of locals supported by the JVM (65,535).
 */
public class WasmModuleDecoder {
    public static final long SPLIT_BIT_MASK = 0x00_00_00_00_10_00_00_00L;
    public static final byte[] MAGIC = new byte[]{0x00, 0x61, 0x73, 0x6D};

    protected final ByteBuffer wasm;

    public WasmModuleDecoder(ByteBuffer wasm){
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

    public <T> T[] decodeVec(IntFunction<T[]> factory, Supplier<T> elementDecoder) throws WasmFormatException {
        var length = decodeU32();
        var array = factory.apply(length);
        for(int i = 0; i < length; i++){
            array[i] = elementDecoder.get();
        }
        return array;
    }

    public byte[] decodeByteVec() throws WasmFormatException {
        var length = decodeU32();
        return readBytes(length);
    }

    public byte[] readBytes(int count) {
        var array = new byte[count];
        wasm.get(array);
        return array;
    }

    public String decodeName() throws WasmFormatException {
        return StandardCharsets.UTF_8
                .decode(ByteBuffer.wrap(decodeByteVec()))
                .toString();
    }

    public int decodeId() throws WasmFormatException {
        return decodeU32();
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

    public FunctionType decodeFunctionType() throws WasmFormatException {
        var value = wasm.get();
        return switch (value){
            case 0x60 -> new FunctionType(
                    decodeVec(ValueType[]::new, this::decodeValType),
                    decodeVec(ValueType[]::new, this::decodeValType));
            default -> throw new WasmFormatException(value, "function type");
        };
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

    public ImportDescriptor decodeImportDescriptor() throws WasmFormatException {
        var value = wasm.get();
        var payload = switch (value){
            case 0x00 -> new TypeId(decodeId());
            case 0x01 -> decodeTableType();
            case 0x02 -> new MemoryType(decodeLimits());
            case 0x03 -> decodeGlobalType();
            default -> throw new WasmFormatException(value, "import descriptor type");
        };
        return new ImportDescriptor(payload);
    }

    public Import decodeImport() throws WasmFormatException {
        return new Import(decodeName(), decodeName(), decodeImportDescriptor());
    }

    public ExportDescriptor decodeExportDescriptor() throws WasmFormatException {
        var value = wasm.get();
        var payload = switch (value){
            case 0x00 -> new FunctionId(decodeId());
            case 0x01 -> new TableId(decodeId());
            case 0x02 -> new MemoryId(decodeId());
            case 0x03 -> new GlobalId(decodeId());
            default -> throw new WasmFormatException(value, "export descriptor type");
        };
        return new ExportDescriptor(payload);
    }

    public Export decodeExport() throws WasmFormatException {
        return new Export(decodeName(), decodeExportDescriptor());
    }

    public Code decodeCode() throws WasmFormatException {
        var codeSize = decodeU32();
        var binaryFunc = readBytes(codeSize);
        return new Code(binaryFunc);
    }

    public TableType decodeTableType() throws WasmFormatException {
        var value = wasm.get();
        return switch (value){
            case 0x70 -> new TableType(ElementType.FUNC_REF, decodeLimits());
            default -> throw new WasmFormatException(value, "table type");
        };
    }

    public MemoryType decodeMemoryType() throws WasmFormatException {
        return new MemoryType(decodeLimits());
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

    public WasmBinaryModule decodeModule() throws WasmFormatException {

        var module = new WasmBinaryModule();

        while(wasm.hasRemaining()){
            var id = wasm.get();
            switch (id) {
                case 0x00 -> module.addCustomSection(decodeCustomSection());
                case 0x01 -> module.setTypeSection(decodeTypeSection());
                case 0x02 -> module.setImportSection(decodeImportSection());
                case 0x03 -> module.setFunctionSection(decodeFunctionSection());
                case 0x04 -> module.setTableSection(decodeTableSection());
                case 0x05 -> module.setMemorySection(decodeMemorySection());
                case 0x06 -> module.setGlobalSection(decodeGlobalSection());
                case 0x07 -> module.setExportSection(decodeExportSection());
                case 0x08 -> module.setStart(decodeStart());
                case 0x09 -> module.setElementSection(decodeElementSection());
                case 0x10 -> module.setCodeSection(decodeCodeSection());
                case 0x11 -> module.setDataSection(decodeDataSection());
                default -> throw new WasmFormatException(id, "module section");
            }
        }

        return module;
    }

    public CustomSection decodeCustomSection() throws WasmFormatException {
        var sectionSize = decodeU32();
        // name is a byte vector, so peek ahead so that we know how many bytes
        // it will take up - that way we can isolate the contents of the custom
        // section as a byte array.
        int pos = wasm.position();
        var nameSize = decodeU32();
        wasm.position(pos);
        return new CustomSection(decodeName(), readBytes(sectionSize - nameSize));
    }

    public FunctionType[] decodeTypeSection() throws WasmFormatException {
        var sectionSize = decodeU32();
        return decodeVec(FunctionType[]::new, this::decodeFunctionType);
    }

    public Import[] decodeImportSection() throws WasmFormatException {
        var sectionSize = decodeU32();
        return decodeVec(Import[]::new, this::decodeImport);
    }

    public TypeId[] decodeFunctionSection() throws WasmFormatException {
        var sectionSize = decodeU32();
        return decodeVec(TypeId[]::new, () -> new TypeId(decodeId()));
    }

    public TableType[] decodeTableSection() throws WasmFormatException {
        var sectionSize = decodeU32();
        return decodeVec(TableType[]::new, this::decodeTableType);
    }

    public MemoryType[] decodeMemorySection() throws WasmFormatException {
        var sectionSize = decodeU32();
        return decodeVec(MemoryType[]::new, this::decodeMemoryType);
    }

    public byte[] decodeGlobalSection() throws WasmFormatException {
        var sectionSize = decodeU32();
        return readBytes(sectionSize);
    }

    public Export[] decodeExportSection() throws WasmFormatException {
        var sectionSize = decodeU32();
        return decodeVec(Export[]::new, this::decodeExport);
    }

    public FunctionId decodeStart() throws WasmFormatException {
        var sectionSize = decodeU32();
        return new FunctionId(decodeId());
    }

    public byte[] decodeElementSection() throws WasmFormatException {
        var sectionSize = decodeU32();
        return readBytes(sectionSize);
    }

    public Code[] decodeCodeSection() throws WasmFormatException {
        var sectionSize = decodeU32();
        return decodeVec(Code[]::new, this::decodeCode);
    }

    public byte[] decodeDataSection() throws WasmFormatException {
        var sectionSize = decodeU32();
        return readBytes(sectionSize);
    }
}
