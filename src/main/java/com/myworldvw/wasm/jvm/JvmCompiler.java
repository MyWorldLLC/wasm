package com.myworldvw.wasm.jvm;

import com.myworldvw.wasm.Table;
import com.myworldvw.wasm.WasmExport;
import com.myworldvw.wasm.WasmModule;
import com.myworldvw.wasm.binary.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class JvmCompiler {

    protected final JvmCompilerConfig config;

    public JvmCompiler(JvmCompilerConfig config){
        this.config = config;
    }

    public static void getFromTable(MethodVisitor code, int id){
        code.visitVarInsn(Opcodes.ALOAD, 0);
        code.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(WasmModule.class), "table0", Type.getDescriptor(Table.class));
        code.visitLdcInsn(id);
        code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Table.class), "get",
                Type.getMethodDescriptor(Type.getType(MethodHandle.class), Type.INT_TYPE), false);
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

        for(int i = 0; i < functions.length; i++){

            var function = functions[i];
            var id = new FunctionId(i, i < firstLocalFunctionId);
            var type = module.typeForFunction(id);

            // Make static invoker helper
            // ============================= Invoker =============================
            var invoker = moduleWriter.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                    "call$" + function.name(), invokerHelperDescriptor(function.type(), module.getName()), null, null);

            invoker.visitVarInsn(Opcodes.ALOAD, function.type().params().length); // index of appended module ref

            loadParams(invoker, function.type(), true);

            invoker.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    module.getName(), function.name(),
                    typeToDescriptor(function.type()), false);

            makeReturn(invoker, function.type().returnType());

            invoker.visitEnd();
            invoker.visitMaxs(0, 0);
            // ============================= End Invoker =============================

            // If imported, make a field for the MethodHandle
            if(function.imported()){
                moduleWriter.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                                function.name(), Type.getDescriptor(MethodHandle.class), null, null)
                        .visitEnd();
            }

            // Visit the method locally implementing this function.
            // If imported, this will call the imported method handle,
            // if local, this will contain the code for this function.
            var methodWriter = moduleWriter.visitMethod(
                    function.exported() ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE,
                    function.name(),
                    typeToDescriptor(type), null, null);

            if(function.exported()){
                var exportVisitor = methodWriter.visitAnnotation(Type.getDescriptor(WasmExport.class), true);
                exportVisitor.visit("functionId", id.id());
                exportVisitor.visitEnd();
            }

            methodWriter.visitCode();

            if(function.imported()){
                // If imported, get the MethodHandle and invoke it
                methodWriter.visitVarInsn(Opcodes.ALOAD, 0);
                methodWriter.visitFieldInsn(Opcodes.GETFIELD,
                        module.getName(), function.name(), Type.getDescriptor(MethodHandle.class));

                loadParams(methodWriter, type, false);
                methodWriter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(MethodHandle.class),
                        "invokeExact", typeToDescriptor(type), false);
                makeReturn(methodWriter, type.returnType());
            }else{
                // If local, compile the function body
                var code = module.getCodeSection();
                var decoder = new WasmFunctionDecoder(code[i - firstLocalFunctionId], module.typeForFunction(id));
                decoder.decode(new JvmCodeVisitor(module, module.getName(), functions, methodWriter)); // TODO - support packaged/prefixed class name
            }

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
                    .forEach(f -> functions.add(makeFunctionInfo(module, new FunctionId(functions.size(), true), Optional.of(f.descriptor().typeId()))));
        }

        if(module.getFunctionSection() != null){
            Arrays.stream(module.getFunctionSection())
                    .forEach(f ->
                        functions.add(makeFunctionInfo(module, new FunctionId(functions.size(), false), Optional.empty()))
                    );
        }

        return functions.toArray(FunctionInfo[]::new);
    }

    protected FunctionInfo makeFunctionInfo(WasmBinaryModule module, FunctionId id, Optional<TypeId> importedType){

        var types = module.getTypeSection();

        var export = module.getExportedName(id);
        var isExported = export.isPresent();
        var name = export.orElse("function$" + id.id());

        var type = importedType.map(typeId -> types[typeId.id()])
                .orElseGet(() -> module.typeForFunction(id));

        return new FunctionInfo(module.getName(), name, type, importedType.isPresent(), isExported);
    }

    public static Type toJvmType(ValueType t){
        return switch (t){
            case I32 -> Type.INT_TYPE;
            case I64 -> Type.LONG_TYPE;
            case F32 -> Type.FLOAT_TYPE;
            case F64 -> Type.DOUBLE_TYPE;
        };
    }

    public static String classNameToDescriptor(String name){
        return "L" + name.replace('.', '/') + ";";
    }

    public static Type[] toJvmTypes(ValueType[] v){
        return Arrays.stream(v).map(JvmCompiler::toJvmType).toArray(Type[]::new);
    }

    public static String typeToDescriptor(FunctionType type){
        return Type.getMethodDescriptor(
                type.isVoid() ? Type.VOID_TYPE : toJvmType(type.results()[0]),
                Arrays.stream(type.params()).map(JvmCompiler::toJvmType).toArray(Type[]::new)
        );
    }

    public static String invokerHelperDescriptor(FunctionType type, String moduleClassName){
        var types = toJvmTypes(type.params());
        var pTypes = Arrays.copyOf(types, types.length + 1);
        pTypes[pTypes.length - 1] = Type.getType(JvmCompiler.classNameToDescriptor(moduleClassName));

        var rType = type.isVoid() ? Type.VOID_TYPE : JvmCompiler.toJvmType(type.results()[0]);
        return Type.getMethodDescriptor(rType, pTypes);
    }

    public static void loadParams(MethodVisitor code, FunctionType type, boolean isStatic){
        var offset = isStatic ? 0 : 1;
        for(int i = 0; i < type.params().length; i++){
            var pType = type.params()[i];
            switch (pType){
                case I32 -> code.visitVarInsn(Opcodes.ILOAD, i + offset);
                case I64 -> code.visitVarInsn(Opcodes.LLOAD, i + offset);
                case F32 -> code.visitVarInsn(Opcodes.FLOAD, i + offset);
                case F64 -> code.visitVarInsn(Opcodes.DLOAD, i + offset);
            }
        }
    }

    public static void makeReturn(MethodVisitor code, Optional<ValueType> rType){
        rType.ifPresentOrElse(
                t -> {
                    switch (t) {
                        case I32 -> code.visitInsn(Opcodes.IRETURN);
                        case F32 -> code.visitInsn(Opcodes.FRETURN);
                        case I64 -> code.visitInsn(Opcodes.LRETURN);
                        case F64 -> code.visitInsn(Opcodes.DRETURN);
                    }
                }, () -> code.visitInsn(Opcodes.RETURN));
    }

}
