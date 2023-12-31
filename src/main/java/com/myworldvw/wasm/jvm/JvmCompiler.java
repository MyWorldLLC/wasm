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

package com.myworldvw.wasm.jvm;

import com.myworldvw.wasm.*;
import com.myworldvw.wasm.binary.*;
import com.myworldvw.wasm.globals.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JvmCompiler {

    protected final WasmConfig config;
    protected final WasmClassLoader loader;

    public JvmCompiler(WasmConfig config, WasmClassLoader loader){
        this.config = config;
        this.loader = loader;
    }

    public static void getFromTable(MethodVisitor code, int id){
        code.visitVarInsn(Opcodes.ALOAD, 0);
        code.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(WasmModule.class), "table0", Type.getDescriptor(Table.class));
        code.visitLdcInsn(id);
        code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Table.class), "get",
                Type.getMethodDescriptor(Type.getType(MethodHandle.class), Type.INT_TYPE), false);
    }

    public String getInternalClassName(String name){
        return getCompiledModuleName(name).replace('.', '/');
    }

    public String getCompiledModuleName(String name){
        if(config.getCompiledModulePackage() != null){
            name = config.getCompiledModulePackage() + "." + name;
        }
        return name;
    }

    public byte[] compile(WasmBinaryModule module) throws WasmFormatException {

        var moduleName = getInternalClassName(module.getName());

        var functions = buildFunctionTable(module);

        var moduleWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        moduleWriter.visit(Opcodes.V19, Opcodes.ACC_PUBLIC, moduleName, null, Type.getInternalName(WasmModule.class), null);

        var constructor = moduleWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(Import[].class)), null, null);
        constructor.visitCode();

        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitVarInsn(Opcodes.ALOAD, 1);
        constructor.visitVarInsn(Opcodes.ALOAD, 2);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(WasmModule.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(Import[].class)), false);

        constructor.visitInsn(Opcodes.RETURN);

        constructor.visitEnd();
        constructor.visitMaxs(0, 0);

        var initializer = moduleWriter.visitMethod(Opcodes.ACC_PUBLIC, "initialize",
                Type.getMethodDescriptor(Type.VOID_TYPE), null, null);
        initializer.visitCode();

        // generate global fields (and initialization code for local globals)
        var globals = generateGlobals(moduleWriter, moduleName, initializer, module, functions);

        if(module.getElementSection() != null){
            generateElements(moduleName, initializer, module, functions, globals);
        }

        if(module.getDataSection() != null){
            generateData(moduleName, initializer, module, functions, globals);
        }

        initializer.visitInsn(Opcodes.RETURN);

        initializer.visitEnd();
        initializer.visitMaxs(0, 0);

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
                    "call$" + function.name(), invokerHelperDescriptor(function.type(), moduleName), null, null);

            invoker.visitVarInsn(Opcodes.ALOAD, function.type().params().length); // index of appended module ref

            loadParams(invoker, function.type(), true);

            invoker.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    moduleName, function.name(),
                    typeToDescriptor(function.type()), false);

            makeReturn(invoker, function.type().returnType());

            invoker.visitEnd();
            invoker.visitMaxs(0, 0);
            // ============================= End Invoker =============================

            // If imported, make a field for the MethodHandle
            if(function.imported()){
                var importField = moduleWriter.visitField(Opcodes.ACC_PUBLIC,
                                function.name(), Type.getDescriptor(MethodHandle.class), null, null);

                var requiredImport = module.getImport(id).get();

                var importVisitor = importField.visitAnnotation(Type.getDescriptor(WasmImport.class), true);
                importVisitor.visit("module", requiredImport.module());
                importVisitor.visit("name", requiredImport.name());
                importVisitor.visitEnd();

                importField.visitEnd();
            }

            // Visit the method locally implementing this function.
            // If imported, this will call the imported method handle,
            // if local, this will contain the code for this function.
            var methodWriter = moduleWriter.visitMethod(
                    function.exported() ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE,
                    function.name(),
                    typeToDescriptor(type), null, null);

            var functionAnnotation = methodWriter.visitAnnotation(Type.getDescriptor(WasmFunction.class), true);
            functionAnnotation.visit("id", id.id());
            functionAnnotation.visitEnd();

            if(function.exported()){
                var exportVisitor = methodWriter.visitAnnotation(Type.getDescriptor(WasmExport.class), true);
                exportVisitor.visitEnd();
            }

            if(module.getStart() != null && module.getStart().equals(id)){
                methodWriter.visitAnnotation(Type.getDescriptor(WasmStart.class), true)
                        .visitEnd();
            }

            methodWriter.visitCode();

            if(function.imported()){
                // If imported, get the MethodHandle and invoke it
                methodWriter.visitVarInsn(Opcodes.ALOAD, 0);
                methodWriter.visitFieldInsn(Opcodes.GETFIELD,
                        moduleName, function.name(), Type.getDescriptor(MethodHandle.class));

                loadParams(methodWriter, type, false);
                methodWriter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(MethodHandle.class),
                        "invokeExact", typeToDescriptor(type), false);
                makeReturn(methodWriter, type.returnType());
            }else{
                // If local, compile the function body
                var code = module.getCodeSection();
                var decoder = new WasmFunctionDecoder(code[i - firstLocalFunctionId], module.typeForFunction(id));
                decoder.decode(new JvmCodeVisitor(module, moduleName, functions, globals, methodWriter));
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

    public List<GlobalInfo> generateGlobals(ClassWriter moduleWriter, String moduleClassName, MethodVisitor moduleInit, WasmBinaryModule module, FunctionInfo[] functions){

        var globals = new ArrayList<GlobalInfo>();

        int id = 0;
        if(module.getImportSection() != null){

            var globalImports = Arrays.stream(module.getImportSection())
                    .filter(i -> i.descriptor().type() == ImportDescriptor.Type.GLOBAL_TYPE)
                    .toArray(Import[]::new);

            for(var i : globalImports){
                var export = module.getExportedGlobalName(new GlobalId(id));
                // WasmContext will initialize, so we don't need to
                var fieldName = generateGlobalField(moduleWriter, id, i.descriptor().globalType(), i, export.orElse(null));

                globals.add(new GlobalInfo(i.module(), i.name(), fieldName, i.descriptor().globalType()));

                id++;
            }
        }

        if(module.getGlobalSection() != null){

            var decoder = new WasmGlobalDecoder(module.getGlobalSection());

            var localGlobalCount = decoder.decodeGlobalCount();
            for(int i = 0; i < localGlobalCount; i++, id++){
                var export = module.getExportedGlobalName(new GlobalId(id));
                var type = decoder.decodeGlobalType();
                var fieldName = generateGlobalField(moduleWriter, id, type, null, export.orElse(null));

                var jvmStorageType = switch (type.valueType()){
                    case I32 -> I32Global.class;
                    case I64 -> I64Global.class;
                    case F32 -> F32Global.class;
                    case F64 -> F64Global.class;
                };

                var jvmParamType = switch (type.valueType()){
                    case I32 -> int.class;
                    case I64 -> long.class;
                    case F32 -> float.class;
                    case F64 -> double.class;
                };

                var factoryMethod = switch (type.mutability()){
                    case VAR -> "mutable";
                    case CONST -> "immutable";
                };

                moduleInit.visitVarInsn(Opcodes.ALOAD, 0);
                var ranInit = decoder.decodeInitializer(new JvmCodeVisitor(module, moduleClassName, functions, globals, moduleInit));

                var params = ranInit
                        ? new Type[]{Type.getType(jvmParamType)}
                        : new Type[]{};

                moduleInit.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(jvmStorageType),
                        factoryMethod,
                        Type.getMethodDescriptor(Type.getType(jvmStorageType), params),
                        false);
                moduleInit.visitFieldInsn(Opcodes.PUTFIELD, moduleClassName, fieldName, Type.getDescriptor(jvmStorageType));

                globals.add(new GlobalInfo(null, null, fieldName, type));
            }

        }

        for(var global : globals){
            generateStaticGlobalAccessor(moduleWriter, moduleClassName, global, true);
            generateStaticGlobalAccessor(moduleWriter, moduleClassName, global, false);
        }

        return globals;
    }

    public String generateGlobalField(ClassWriter moduleWriter, int id, GlobalType type, Import i, String exportName){
        var imported = i != null;
        var exported = exportName != null;
        var name = exported ? exportName : "global$" + id;

        var storageType = globalType(type.valueType());

        var field = moduleWriter.visitField(
                exported || imported ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE,
                name,
                Type.getDescriptor(storageType),
                null,
                null
        );

        if(imported){
            var annotation = field.visitAnnotation(Type.getDescriptor(WasmImport.class), true);
            annotation.visit("module", i.module());
            annotation.visit("name", i.name());
            annotation.visitEnd();
        }

        if(exported){
            var annotation = field.visitAnnotation(Type.getDescriptor(WasmExport.class), true);
            annotation.visit("id", id);
            annotation.visitEnd();
        }

        return name;
    }

    public static void generateStaticGlobalAccessor(ClassWriter moduleWriter, String moduleClassName, GlobalInfo global, boolean set){
        var methodName = (set ? "set$" : "get$") + global.fieldName();
        var invoker = moduleWriter.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                methodName, globalAccessorHelperDescriptor(set, global.type().valueType(), moduleClassName), null, null);

        // Get the global from the module field, and invoke setValue()
        var moduleParam = set ? 1 : 0;
        invoker.visitVarInsn(Opcodes.ALOAD, moduleParam);
        invoker.visitFieldInsn(Opcodes.GETFIELD, moduleClassName, global.fieldName(),
                Type.getDescriptor(globalType(global.type().valueType())));

        if(set){
            // invoke the global carrier's set method
            invoker.visitVarInsn(loadOpcode(global.type().valueType()), 0);
            invoker.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(globalType(global.type().valueType())),
                    "setValue",
                    Type.getMethodDescriptor(Type.VOID_TYPE, toJvmType(global.type().valueType())),
                    false
            );

            makeReturn(invoker, Optional.empty());
        }else{
            // invoke the global carrier's get method
            invoker.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(globalType(global.type().valueType())),
                    "getValue",
                    Type.getMethodDescriptor(toJvmType(global.type().valueType())),
                    false
            );
            makeReturn(invoker, Optional.of(global.type().valueType()));
        }

        invoker.visitEnd();
        invoker.visitMaxs(0, 0);
    }

    public void generateElements(String moduleClassName, MethodVisitor moduleInit, WasmBinaryModule module, FunctionInfo[] functions, List<GlobalInfo> globals){
        var decoder = new WasmElementsDecoder(module.getElementSection());

        var elementCounts = decoder.decodeElementCount();
        for(int i = 0; i < elementCounts; i++){
            moduleInit.visitVarInsn(Opcodes.ALOAD, 0);

            moduleInit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(WasmModule.class),
                    "getTable", Type.getMethodDescriptor(Type.getType(Table.class)), false);

            // Evaluate offset
            decoder.decodeOffsetExpr(new JvmCodeVisitor(module, moduleClassName, functions, globals, moduleInit));

            var idVec = decoder.decodeIds();

            // Build MethodHandle[] of the functions
            moduleInit.visitLdcInsn(idVec.length);
            moduleInit.visitTypeInsn(Opcodes.ANEWARRAY, Type.getDescriptor(MethodHandle.class));
            for(int id = 0; id < idVec.length; id++){
                moduleInit.visitInsn(Opcodes.DUP);
                moduleInit.visitLdcInsn(id);

                // Get method handle
                moduleInit.visitVarInsn(Opcodes.ALOAD, 0);
                moduleInit.visitLdcInsn(idVec[id]);
                moduleInit.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(WasmContext.class),
                        "getFunctionHandleDirect",
                        Type.getMethodDescriptor(Type.getType(MethodHandle.class),
                                Type.getType(WasmModule.class), Type.getType(int.class)), false);

                moduleInit.visitInsn(Opcodes.AASTORE);
            }

            moduleInit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Table.class),
                    "setAll",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(MethodHandle[].class)), false);
        }
    }

    public void generateData(String moduleClassName, MethodVisitor moduleInit, WasmBinaryModule module, FunctionInfo[] functions, List<GlobalInfo> globals){
        var decoder = new WasmDataDecoder(module.getDataSection());

        var dataCount = decoder.decodeDataCount();
        for(int i = 0; i < dataCount; i++){

            // Unused for now, since there is at most one memory segment per module
            var memoryId = decoder.decodeMemoryId();

            moduleInit.visitVarInsn(Opcodes.ALOAD, 0);

            moduleInit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(WasmModule.class),
                    "getMemory", Type.getMethodDescriptor(Type.getType(Memory.class)), false);

            decoder.decodeOffsetExpr(new JvmCodeVisitor(module, moduleClassName, functions, globals, moduleInit));

            var data = decoder.decodeData();
            moduleInit.visitLdcInsn(data.length);
            moduleInit.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
            // Unfortunately there doesn't seem to be a better way to encode
            // a potentially large data segment into the class file, though perhaps
            // we should support handing off the byte arrays to the host to initialize
            // the memory through more efficient means.
            for(int b = 0; b < data.length; b++){
                moduleInit.visitInsn(Opcodes.DUP);
                moduleInit.visitLdcInsn(b);
                moduleInit.visitLdcInsn(data[b]);
                moduleInit.visitInsn(Opcodes.BASTORE);
            }

            moduleInit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Memory.class),
                    "bulkSet", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(byte[].class)),
                    false);
        }

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

    public static Class<? extends Global<?>> globalType(ValueType type){
        return switch (type) {
            case I32 -> I32Global.class;
            case I64 -> I64Global.class;
            case F32 -> F32Global.class;
            case F64 -> F64Global.class;
        };
    }

    public static String globalAccessorHelperDescriptor(boolean set, ValueType type, String moduleClassName){
        var gType = toJvmType(type);
        return set ? Type.getMethodDescriptor(Type.VOID_TYPE, gType, Type.getType(classNameToDescriptor(moduleClassName)))
                : Type.getMethodDescriptor(gType, Type.getType(classNameToDescriptor(moduleClassName)));
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

    public static int loadOpcode(ValueType type){
        return switch (type){
                case I32 -> Opcodes.ILOAD;
                case I64 -> Opcodes.LLOAD;
                case F32 -> Opcodes.FLOAD;
                case F64 -> Opcodes.DLOAD;
            };
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
