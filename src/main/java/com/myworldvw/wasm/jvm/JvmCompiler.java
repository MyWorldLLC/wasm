package com.myworldvw.wasm.jvm;

import com.myworldvw.wasm.binary.ValueType;
import com.myworldvw.wasm.binary.WasmDecoder;
import com.myworldvw.wasm.binary.WasmFormatException;
import org.objectweb.asm.ClassWriter;

import java.nio.ByteBuffer;

public class JvmCompiler {

    protected final JvmCompilerConfig config;
    protected final WasmDecoder decoder;

    public JvmCompiler(JvmCompilerConfig config, ByteBuffer wasm){
        this.config = config;
        decoder = new WasmDecoder(wasm);
    }

    public Class<?> compile() throws WasmFormatException {

        decoder.begin();

        var moduleWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);



        return null;
    }

}
