package com.myworldvw.wasm.jvm;

import com.myworldvw.wasm.WasmExport;
import com.myworldvw.wasm.WasmModule;
import com.myworldvw.wasm.binary.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;

public class JvmCompiler {

    protected final JvmCompilerConfig config;

    public JvmCompiler(JvmCompilerConfig config){
        this.config = config;
    }

    public WasmClassLoader getLoader(){
        return config.loader;
    }

    public byte[] compile(WasmBinaryModule module) throws WasmFormatException {

        var moduleWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        moduleWriter.visit(Opcodes.V19, Opcodes.ACC_PUBLIC, module.getName(), null, Type.getInternalName(WasmModule.class), null);

        // TODO - create table.

        var constructor = moduleWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(Import[].class)), null, null);
        constructor.visitCode();

        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitVarInsn(Opcodes.ALOAD, 1);
        constructor.visitVarInsn(Opcodes.ALOAD, 2);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(WasmModule.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(Import[].class)), false);

        // TODO - visit global, element, & data sections & perform applicable initialization.

        constructor.visitInsn(Opcodes.RETURN);

        constructor.visitEnd();
        constructor.visitMaxs(0, 0);

        // Visit functions. Exported functions will be public and annotated with @WasmExport,
        // non-exported functions should be private.

        var types = module.getTypeSection();
        var code = module.getCodeSection();
        for(int i = 0; i < code.length; i++){
            var id = new FunctionId(i);
            var export = module.getExportedName(id);

            var access = export.isPresent() ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE;

            var name = module.getExportedName(id).orElse("function$" + id.id());

            var methodWriter = moduleWriter.visitMethod(access, name, typeToDescriptor(types[i]), null, null);
            if(export.isPresent()){
                var exportVisitor = methodWriter.visitAnnotation(Type.getDescriptor(WasmExport.class), true);
                    exportVisitor.visit("functionId", i);
                    exportVisitor.visitEnd();
            }

            methodWriter.visitCode();
            var decoder = new WasmFunctionDecoder(code[i], types[i]);
            decoder.decode(new JvmCodeVisitor(module.getName(), methodWriter));

            methodWriter.visitMaxs(0, 0);
            methodWriter.visitEnd();
        }

        moduleWriter.visitEnd();

        return moduleWriter.toByteArray();
    }

    public static Type toJvmType(ValueType t){
        return switch (t){
            case I32 -> Type.INT_TYPE;
            case I64 -> Type.LONG_TYPE;
            case F32 -> Type.FLOAT_TYPE;
            case F64 -> Type.DOUBLE_TYPE;
        };
    }

    public String typeToDescriptor(FunctionType type){
        return Type.getMethodDescriptor(
                type.isVoid() ? Type.VOID_TYPE : toJvmType(type.results()[0]),
                Arrays.stream(type.params()).map(JvmCompiler::toJvmType).toArray(Type[]::new)
        );
    }

}
