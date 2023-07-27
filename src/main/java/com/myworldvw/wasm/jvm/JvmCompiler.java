package com.myworldvw.wasm.jvm;

import com.myworldvw.wasm.WasmExport;
import com.myworldvw.wasm.WasmModule;
import com.myworldvw.wasm.binary.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

public class JvmCompiler {

    protected final JvmCompilerConfig config;

    public JvmCompiler(JvmCompilerConfig config){
        this.config = config;
    }

    public WasmClassLoader getLoader(){
        return config.loader;
    }

    public byte[] compile(WasmBinaryModule module) throws WasmFormatException {

        var functions = buildFunctionTable(module);

        var moduleWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        moduleWriter.visit(Opcodes.V19, Opcodes.ACC_PUBLIC, module.getName(), null, Type.getInternalName(WasmModule.class), null);

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
        // non-exported functions should be private. Note that imported functions may also be exported.
        // For each function (imported or local), create a static invoker helper that takes the target
        // function's parameter list and then the module instance as the last parameter, and invokes
        // the target function. We do this because the stack will be prepared with the parameters
        // for the target function, but the JVM expects the instance receiving the call to be on
        // the stack first. Performance impacts of this strategy should be negligible, as these
        // extra layers are quite thin and the JIT should be able to inline them easily.

        var firstLocalFunctionId = (int) Arrays.stream(functions)
                .filter(FunctionInfo::imported)
                .count();

        // TODO - make fields for imported functions,
        // and make call methods for them

        var types = module.getTypeSection();
        var code = module.getCodeSection();
        for(int i = 0; i < code.length; i++){
            var id = new FunctionId(firstLocalFunctionId + i);
            var function = functions[id.id()];

            var access = function.exported() ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE;

            var methodWriter = moduleWriter.visitMethod(access, function.name(), typeToDescriptor(types[i]), null, null);
            if(function.exported()){
                var exportVisitor = methodWriter.visitAnnotation(Type.getDescriptor(WasmExport.class), true);
                    exportVisitor.visit("functionId", id.id());
                    exportVisitor.visitEnd();
            }

            methodWriter.visitCode();
            var decoder = new WasmFunctionDecoder(code[i], types[i]);
            decoder.decode(new JvmCodeVisitor(module, module.getName(), functions, methodWriter)); // TODO - support packaged/prefixed class name

            methodWriter.visitMaxs(0, 0);
            methodWriter.visitEnd();
        }

        moduleWriter.visitEnd();

        return moduleWriter.toByteArray();
    }

    public FunctionInfo[] buildFunctionTable(WasmBinaryModule module){
        var functions = new ArrayList<FunctionInfo>();

        if(module.getImportSection() != null){
            Arrays.stream(module.getImportSection())
                    .filter(i -> i.descriptor().type() == ImportDescriptor.Type.TYPE_ID)
                    .map(i -> makeFunctionInfo(module, new FunctionId(functions.size() + 1), true))
                    .forEach(functions::add);
        }

        if(module.getFunctionSection() != null){
            Arrays.stream(module.getFunctionSection())
                    .map(f -> makeFunctionInfo(module, new FunctionId(functions.size() + 1), false))
                    .forEach(functions::add);
        }

        return functions.toArray(FunctionInfo[]::new);
    }

    protected FunctionInfo makeFunctionInfo(WasmBinaryModule module, FunctionId id, boolean isImported){

        var types = module.getTypeSection();

        var export = module.getExportedName(id);
        var isExported = export.isPresent();
        var name = export.orElse("function$" + id.id());

        return new FunctionInfo(module.getName(), name, types[id.id()], isImported, isExported);
    }

    public static Type toJvmType(ValueType t){
        return switch (t){
            case I32 -> Type.INT_TYPE;
            case I64 -> Type.LONG_TYPE;
            case F32 -> Type.FLOAT_TYPE;
            case F64 -> Type.DOUBLE_TYPE;
        };
    }

    public static String typeToDescriptor(FunctionType type){
        return Type.getMethodDescriptor(
                type.isVoid() ? Type.VOID_TYPE : toJvmType(type.results()[0]),
                Arrays.stream(type.params()).map(JvmCompiler::toJvmType).toArray(Type[]::new)
        );
    }

}
