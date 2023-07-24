package com.myworldvw.wasm.jvm;

import com.myworldvw.wasm.binary.WasmModuleDecoder;
import com.myworldvw.wasm.binary.WasmFormatException;
import org.objectweb.asm.ClassWriter;

import java.nio.ByteBuffer;

public class JvmCompiler {

    protected final JvmCompilerConfig config;
    protected final WasmModuleDecoder decoder;

    public JvmCompiler(JvmCompilerConfig config, ByteBuffer wasm){
        this.config = config;
        decoder = new WasmModuleDecoder(wasm);
    }

    public Class<?> compile() throws WasmFormatException {

        decoder.begin();

        var moduleWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);



        return null;
    }

}
