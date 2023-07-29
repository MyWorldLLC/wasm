package com.myworldvw.wasm.jvm;

import com.myworldvw.wasm.Memory;
import com.myworldvw.wasm.WasmModule;
import com.myworldvw.wasm.binary.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.util.*;

import static com.myworldvw.wasm.binary.WasmOpcodes.*;

public class JvmCodeVisitor implements CodeVisitor {

    enum BlockType {BLOCK, LOOP, IF}
    record BlockInfo(BlockType type, int stackDepth, Label label, Label elseTarget){}

    protected final WasmBinaryModule module;
    protected final String moduleClassName;
    protected final FunctionInfo[] functionTable;
    protected final List<GlobalInfo> globalTable;

    protected final MethodVisitor code;
    protected FunctionType signature;
    protected final Deque<Optional<ValueType>> blockTypes;
    protected final Deque<BlockInfo> blockLabels;
    protected final Deque<ValueType> operands;
    protected ValueType[] locals;

    public JvmCodeVisitor(WasmBinaryModule module, String moduleClassName, FunctionInfo[] functionTable, List<GlobalInfo> globalTable, MethodVisitor code){
        this.module = module;
        this.moduleClassName = moduleClassName;
        this.functionTable = functionTable;
        this.globalTable = globalTable;
        this.code = code;
        blockTypes = new ArrayDeque<>();
        blockLabels = new ArrayDeque<>();
        operands = new ArrayDeque<>();
    }

    public Optional<ValueType> peek(){
        return operands.isEmpty() ? Optional.empty() : Optional.of(operands.peek());
    }

    protected void push(ValueType t){
        operands.push(t);
    }

    protected ValueType pop(){
        return operands.pop();
    }

    protected Label getLabel(int index){
        int current = 0;
        for(var block : blockLabels){
            if(current == index){
                return block.label();
            }
            current++;
        }
        return null; // If wasm is well-formed, this will never happen
    }

    @Override
    public void visitLocals(ValueType[] locals) {
        this.locals = locals;
    }

    @Override
    public void exitBlock() {
        // Exiting an internal block
        var block = blockLabels.pop();
        if(block.type() == BlockType.BLOCK){
            code.visitLabel(block.label());
        }
        while (operands.size() > block.stackDepth()){
            operands.pop();
            pop();
        }
    }

    @Override
    public void exitFunction(){
        // Exiting the function body
        makeReturn();
    }

    @Override
    public void visitFunction(FunctionType type) {
        signature = type;
    }

    @Override
    public void visitBlock(byte opcode, Optional<ValueType> blockType) {

        if(opcode == ELSE){
            // If bytecode is well-formed, this will never NPE.
            code.visitLabel(blockLabels.peek().elseTarget());
            return;
        }

        blockTypes.push(blockType);

        var label = new Label();
        var elseTarget = new Label();

        if(opcode == LOOP){
            code.visitLabel(label);
        }

        var infoType = switch (opcode){
            case BLOCK -> BlockType.BLOCK;
            case LOOP -> BlockType.LOOP;
            case IF -> BlockType.IF;
            default -> BlockType.BLOCK;
        };

        blockLabels.push(new BlockInfo(infoType, operands.size(), label, elseTarget));

        if(opcode == IF){
            code.visitLdcInsn(0);
            code.visitJumpInsn(Opcodes.IFEQ, elseTarget);
        }
    }

    @Override
    public void visitBranch(byte opcode, int labelId) {
        var target = getLabel(labelId);
        switch (opcode){
            case BR -> code.visitJumpInsn(Opcodes.GOTO, target);
            case BR_IF -> {
                code.visitLdcInsn(0);
                code.visitJumpInsn(Opcodes.IFNE, target);
                pop();
            }
        }
    }

    @Override
    public void visitTableBranch(byte opcode, int[] labelIds, int defaultTarget) {
        // This is only triggered by a BR_TABLE, so no need to test the opcode
        // There is a single integer operand indexing into the label ids.
        var labels = Arrays.stream(labelIds)
                        .mapToObj(this::getLabel)
                        .toArray(Label[]::new);
        code.visitTableSwitchInsn(0, labelIds.length - 1, getLabel(defaultTarget), labels);
    }

    @Override
    public void visitCtrl(byte opcode) {
        switch (opcode){
            case UNREACHABLE, NOP -> {}
            case RETURN -> makeReturn();
        }
    }

    @Override
    public void visitCall(byte opcode, int target) {
        var function = functionTable[target];
        switch (opcode) {
            case CALL -> {
                // Invoke target via the module's static invoker helper for that function
                code.visitVarInsn(Opcodes.ALOAD, 0);
                code.visitMethodInsn(Opcodes.INVOKESTATIC, moduleClassName,
                        "call$" + function.name(),
                        JvmCompiler.invokerHelperDescriptor(function.type(), moduleClassName), false);
            }
            case CALL_INDIRECT -> {
                JvmCompiler.getFromTable(code, target);
                code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(MethodHandle.class),
                        "invokeExact", JvmCompiler.typeToDescriptor(function.type()), false);
            }
        }
    }

    @Override
    public void visitParametric(byte opcode) {
        switch (opcode){
            case DROP -> {
                switch (operands.peek()){
                    case I32, F32 -> code.visitInsn(Opcodes.POP);
                    case I64, F64 -> code.visitInsn(Opcodes.POP2);
                }
                pop();
            }
            case SELECT -> {
                // test top of stack, if 1 get the first value, if 0 get the second

                // last operand is always an i32, so the first two operands will determine
                // the type of the select expression
                pop();
                pop();
                var opType = pop();
                var jvmType = JvmCompiler.toJvmType(opType);
                code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Runtime.class),
                    "select", Type.getMethodDescriptor(jvmType, jvmType, jvmType, Type.INT_TYPE), false);
            }
        }
    }

    @Override
    public void visitVar(byte opcode, int id) {
        switch (opcode){
            case LOCAL_GET -> {
                var type = paramOrLocal(id);
                id = id + 1; // offset by 1 since l0 is always 'this'

                push(type);
                switch (type){
                    case I32 -> code.visitVarInsn(Opcodes.ILOAD, id);
                    case F32 -> code.visitVarInsn(Opcodes.FLOAD, id);
                    case I64 -> code.visitVarInsn(Opcodes.LLOAD, id);
                    case F64 -> code.visitVarInsn(Opcodes.DLOAD, id);
                }
            }
            case LOCAL_SET -> {
                var type = paramOrLocal(id);
                id = id + 1; // offset by 1 since l0 is always 'this'

                pop();
                switch (type){
                    case I32 -> code.visitVarInsn(Opcodes.ISTORE, id);
                    case F32 -> code.visitVarInsn(Opcodes.FSTORE, id);
                    case I64 -> code.visitVarInsn(Opcodes.LSTORE, id);
                    case F64 -> code.visitVarInsn(Opcodes.DSTORE, id);
                }
            }
            case LOCAL_TEE -> {

                var type = paramOrLocal(id);
                id = id + 1; // offset by 1 since l0 is always 'this'

                switch (operands.peek()){
                    case I32, F32 -> code.visitInsn(Opcodes.DUP);
                    case I64, F64 -> code.visitInsn(Opcodes.DUP2);
                }
                switch (type){
                    case I32 -> code.visitVarInsn(Opcodes.ISTORE, id);
                    case F32 -> code.visitVarInsn(Opcodes.FSTORE, id);
                    case I64 -> code.visitVarInsn(Opcodes.LSTORE, id);
                    case F64 -> code.visitVarInsn(Opcodes.DSTORE, id);
                }
            }
            case GLOBAL_GET -> {
                var global = globalTable.get(id);
                push(global.type().valueType());
                makeGlobalAccess(true, global.fieldName(), global.type());
            }
            case GLOBAL_SET -> {
                var global = globalTable.get(id);
                pop();
                makeGlobalAccess(false, global.fieldName(), global.type());
            }
        }
    }

    @Override
    public void visitMemory(byte opcode, int align, int offset) {
        switch (opcode){
            case I32_LOAD -> makeILoad(ValueType.I32, 32, align, offset, true);
            case I64_LOAD -> makeILoad(ValueType.I64, 64, align, offset, true);
            case F32_LOAD -> makeFLoad(ValueType.F32, align, offset);
            case F64_LOAD -> makeFLoad(ValueType.F64, align, offset);
            case I32_LOAD_8_S -> makeILoad(ValueType.I32, 8, align, offset, true);
            case I32_LOAD_8_U -> makeILoad(ValueType.I32, 8, align, offset, false);
            case I32_LOAD_16_S -> makeILoad(ValueType.I32, 16, align, offset, true);
            case I32_LOAD_16_U -> makeILoad(ValueType.I32, 16, align, offset, false);
            case I64_LOAD_8_S -> makeILoad(ValueType.I64, 8, align, offset, true);
            case I64_LOAD_8_U -> makeILoad(ValueType.I64, 8, align, offset, false);
            case I64_LOAD_16_S -> makeILoad(ValueType.I64, 16, align, offset, true);
            case I64_LOAD_16_U -> makeILoad(ValueType.I64, 16, align, offset, false);
            case I64_LOAD_32_S -> makeILoad(ValueType.I64, 32, align, offset, true);
            case I64_LOAD_32_U -> makeILoad(ValueType.I64, 32, align, offset, false);
            case I32_STORE -> makeIStore(ValueType.I32, 32, align, offset);
            case I64_STORE -> makeIStore(ValueType.I64, 64, align, offset);
            case F32_STORE -> makeFStore(ValueType.F32, align, offset);
            case F64_STORE -> makeFStore(ValueType.F64, align, offset);
            case I32_STORE_8 -> makeIStore(ValueType.I32, 8, align, offset);
            case I32_STORE_16 -> makeIStore(ValueType.I32, 16, align, offset);
            case I64_STORE_8 -> makeIStore(ValueType.I64, 8, align, offset);
            case I64_STORE_16 -> makeIStore(ValueType.I64, 16, align, offset);
            case I64_STORE_32 -> makeIStore(ValueType.I64, 32, align, offset);
            case MEMORY_SIZE -> {
                pushMemory();
                code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Memory.class), "size",
                        Type.getMethodDescriptor(Type.INT_TYPE), false);
                push(ValueType.I32);
            }
            case MEMORY_GROW -> {
                pushMemory();
                code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Memory.class), "grow",
                        Type.getMethodDescriptor(Type.INT_TYPE, Type.INT_TYPE), false);
                pop();
                push(ValueType.I32);
            }
        }
    }

    @Override
    public void visitConst(byte opcode, long immediate) {
        switch (opcode){
            case I32_CONST -> {
                code.visitLdcInsn((int) immediate);
                push(ValueType.I32);
            }
            case I64_CONST -> {
                code.visitLdcInsn(immediate);
                push(ValueType.I64);
            }
            case F32_CONST -> {
                code.visitLdcInsn(Float.intBitsToFloat((int) immediate));
                push(ValueType.F32);
            }
            case F64_CONST -> {
                code.visitLdcInsn(Double.longBitsToDouble(immediate));
                push(ValueType.F64);
            }
        }
    }

    @Override
    public void visitNumeric(byte opcode) {
        switch (opcode){
            case I32_EQZ -> {
                code.visitLdcInsn(0);
                push(ValueType.I32);
                testIntEquality(ValueType.I32);
            }
            case I32_EQ -> testIntEquality(ValueType.I32);
            case I32_NE -> testIntInequality(ValueType.I32);
            case I32_ADD -> addInts(ValueType.I32);
        }
    }

    protected void makeReturn(){
        JvmCompiler.makeReturn(code, signature.returnType());
    }

    protected ValueType paramOrLocal(int id){
        if(id < signature.params().length){
            return signature.params()[id];
        }
        return locals[id];
    }

    protected void testIntEquality(ValueType t){
        var testSuccess = new Label();
        var end = new Label();

        code.visitJumpInsn(Opcodes.IF_ICMPEQ, testSuccess);
        code.visitLdcInsn(0);
        code.visitJumpInsn(Opcodes.GOTO, end);
        code.visitLabel(testSuccess);
        code.visitLdcInsn(1);
        code.visitLabel(end);

        if(t == ValueType.I64){
            code.visitInsn(Opcodes.I2L);
        }

        pop();
        pop();
        push(t);
    }

    protected void testIntInequality(ValueType t){
        testIntEquality(t);

        switch (t){
            case I32 -> {
                code.visitLdcInsn(1);
                code.visitInsn(Opcodes.IXOR);
            }
            case I64 -> {
                code.visitLdcInsn(1L);
                code.visitInsn(Opcodes.LXOR);
            }
        }

    }

    protected void compareInt(ValueType t, boolean signed){

        var method = signed ? "compare" : "compareUnsigned";

        if(t == ValueType.I32){
            code.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getDescriptor(Integer.class),
                    method,
                    Type.getMethodDescriptor(Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE), false);
        }else if(t == ValueType.I64){
            code.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getDescriptor(Long.class),
                    method,
                    Type.getMethodDescriptor(Type.INT_TYPE, Type.LONG_TYPE, Type.LONG_TYPE), false);
            code.visitInsn(Opcodes.I2L);
        }

        pop();
        pop();
        push(t);

    }

    protected void addInts(ValueType t){
        switch (t){
            case I32 -> code.visitInsn(Opcodes.IADD);
            case I64 -> code.visitInsn(Opcodes.LADD);
        }

        pop();
        pop();
        push(t);
    }

    protected void pushMemory(){
        code.visitVarInsn(Opcodes.ALOAD, 0);
        code.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(WasmModule.class), "memory0", Type.getDescriptor(Memory.class));
    }

    protected void makeILoad(ValueType target, int storedWidth, int align, int offset, boolean signed){
        // At some point in the future we may use the 'align' argument, but the underlying memory segment
        // already handles enforcing alignment for us so we can safely ignore it for now.
        pushMemory();
        code.visitInsn(Opcodes.SWAP);
        code.visitLdcInsn(offset);
        code.visitInsn(Opcodes.IADD);
        switch (target){
            case I32 -> {
                switch (storedWidth){
                    case 8 -> {
                        code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Memory.class),
                                "readI8", Type.getMethodDescriptor(Type.BYTE_TYPE, Type.INT_TYPE), false);
                        code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Byte.class),
                                signed ? "intValue" : "toUnsignedInt", Type.getMethodDescriptor(Type.INT_TYPE, Type.BYTE_TYPE), false);
                    }
                    case 16 -> {
                        code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Memory.class),
                                "readI16", Type.getMethodDescriptor(Type.SHORT_TYPE, Type.INT_TYPE), false);
                        code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Short.class),
                                signed ? "intValue" : "toUnsignedInt", Type.getMethodDescriptor(Type.INT_TYPE, Type.SHORT_TYPE), false);
                    }
                    case 32 -> {
                        code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Memory.class),
                                "readI32", Type.getMethodDescriptor(Type.INT_TYPE, Type.INT_TYPE), false);
                    }
                }
            }
            case I64 -> {
                switch (storedWidth){
                    case 8 -> {
                        code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Memory.class),
                                "readI8", Type.getMethodDescriptor(Type.BYTE_TYPE, Type.INT_TYPE), false);
                        code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Byte.class),
                                signed ? "longValue" : "toUnsignedLong", Type.getMethodDescriptor(Type.LONG_TYPE, Type.BYTE_TYPE), false);
                    }
                    case 16 -> {
                        code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Memory.class),
                                "readI16", Type.getMethodDescriptor(Type.SHORT_TYPE, Type.INT_TYPE), false);
                        code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Short.class),
                                signed ? "longValue" : "toUnsignedLong", Type.getMethodDescriptor(Type.LONG_TYPE, Type.SHORT_TYPE), false);
                    }
                    case 32 -> {
                        code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Memory.class),
                                "readI32", Type.getMethodDescriptor(Type.INT_TYPE, Type.INT_TYPE), false);
                        code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class),
                                signed ? "longValue" : "toUnsignedLong", Type.getMethodDescriptor(Type.LONG_TYPE, Type.INT_TYPE), false);
                    }
                    case 64 -> {
                        code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Memory.class),
                                "readI64", Type.getMethodDescriptor(Type.LONG_TYPE, Type.INT_TYPE), false);
                    }
                }
            }
        }

        pop();
        push(target);
    }

    protected void makeIStore(ValueType target, int storedWidth, int align, int offset){
        // Note that stores can shorten but never widen the value - that is,
        // a 32-bit can be stored as 16 or 8, but never 64 (likewise for 64-bit)
        // stack: addr, value

        String storageMethod = "";
        Type storageType = Type.LONG_TYPE;

        // If we are storing a 64-bit, first shorten to 32
        if(target == ValueType.I64 && storedWidth < 64){
            code.visitInsn(Opcodes.L2I);
        }

        // At this point we have an integer value on the stack if shortening is happening
        switch (storedWidth){
            case 8 -> {
                // truncate to byte
                code.visitInsn(Opcodes.I2B);
                storageMethod = "staticWriteI8";
                storageType = Type.BYTE_TYPE;
            }
            case 16 -> {
                code.visitInsn(Opcodes.I2S);
                storageMethod = "staticWriteI16";
                storageType = Type.SHORT_TYPE;
            }
            case 32 -> {
                storageMethod = "staticWriteI32";
                storageType = Type.INT_TYPE;
            }
            case 64 -> {
                storageMethod = "staticWriteI64";
                storageType = Type.LONG_TYPE;
            }
        }

        // Shuffling the stack around is quite tricky (especially when dealing with 64-bit values), so use the
        // helper methods. Performance with this should be OK since the JIT will most likely just inline it.
        pushMemory();
        code.visitLdcInsn(offset);
        code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Memory.class), storageMethod,
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, storageType, Type.getType(Memory.class), Type.INT_TYPE), false);

        pop();
        pop();
    }

    protected void makeFLoad(ValueType target, int align, int offset){
        pushMemory();
        code.visitInsn(Opcodes.SWAP);
        code.visitLdcInsn(offset);
        code.visitInsn(Opcodes.IADD);

        switch (target){
            case F32 -> code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Memory.class),
                    "readF32", Type.getMethodDescriptor(Type.FLOAT_TYPE, Type.INT_TYPE), false);
            case F64 -> code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Memory.class),
                    "readF64", Type.getMethodDescriptor(Type.DOUBLE_TYPE, Type.INT_TYPE), false);
        }

        pop();
        push(target);
    }

    protected void makeFStore(ValueType target, int align, int offset){

        String storageMethod = "";
        Type storageType = Type.DOUBLE_TYPE;

        switch (target){
            case F32 -> {
                storageMethod = "staticWriteF32";
                storageType = Type.FLOAT_TYPE;
            }
            case F64 -> {
                storageMethod = "staticWriteF64";
                storageType = Type.DOUBLE_TYPE;
            }
        }

        pushMemory();
        code.visitLdcInsn(offset);
        code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Memory.class), storageMethod,
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, storageType, Type.getType(Memory.class), Type.INT_TYPE), false);

        pop();
        pop();
    }

    protected void makeGlobalAccess(boolean load, String fieldName, GlobalType type){
        var method = load ? "get$" + fieldName : "set$" + fieldName;
        var descriptor = load
                ? Type.getMethodDescriptor(JvmCompiler.toJvmType(type.valueType()), Type.getType(JvmCompiler.classNameToDescriptor(moduleClassName)))
                : Type.getMethodDescriptor(Type.VOID_TYPE, JvmCompiler.toJvmType(type.valueType()), Type.getType(JvmCompiler.classNameToDescriptor(moduleClassName)));

        code.visitVarInsn(Opcodes.ALOAD, 0);
        code.visitMethodInsn(Opcodes.INVOKESTATIC, moduleClassName, method, descriptor, false);
    }
}
