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

    protected BlockInfo getJumpTarget(int index){
        int current = 0;
        for(var block : blockLabels){
            if(current == index){
                return block;
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
        // TODO - check if operand stack needs to be popped as part of any jump

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
            pop();
        }
    }

    @Override
    public void visitBranch(byte opcode, int labelId) {
        var target = getJumpTarget(labelId);
        switch (opcode){
            case BR -> code.visitJumpInsn(Opcodes.GOTO, target.label());
            case BR_IF -> {
                code.visitLdcInsn(0);
                code.visitJumpInsn(Opcodes.IFNE, target.label());
                pop();
            }
        }
    }

    @Override
    public void visitTableBranch(byte opcode, int[] labelIds, int defaultTarget) {
        // This is only triggered by a BR_TABLE, so no need to test the opcode
        // There is a single integer operand indexing into the label ids.
        var labels = Arrays.stream(labelIds)
                        .mapToObj(this::getJumpTarget)
                        .map(BlockInfo::label)
                        .toArray(Label[]::new);
        code.visitTableSwitchInsn(0, labelIds.length - 1, getJumpTarget(defaultTarget).label(), labels);
        pop();
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
            // I32 Tests & Comparisons
            case I32_EQZ -> {
                code.visitLdcInsn(0);
                push(ValueType.I32);
                testIntEquality(ValueType.I32);
            }
            case I32_EQ -> testIntEquality(ValueType.I32);
            case I32_NE -> testIntInequality(ValueType.I32);
            case I32_LT_S -> {
                compare(ValueType.I32, true);
                comparisonHelper(Opcodes.IFLT);
            }
            case I32_LT_U -> {
                compare(ValueType.I32, false);
                comparisonHelper(Opcodes.IFLT);
            }
            case I32_GT_S -> {
                compare(ValueType.I32, true);
                comparisonHelper(Opcodes.IFGT);
            }
            case I32_GT_U -> {
                compare(ValueType.I32, false);
                comparisonHelper(Opcodes.IFGT);
            }
            case I32_LE_S -> {
                compare(ValueType.I32, true);
                comparisonHelper(Opcodes.IFLE);
            }
            case I32_LE_U -> {
                compare(ValueType.I32, false);
                comparisonHelper(Opcodes.IFLE);
            }
            case I32_GE_S -> {
                compare(ValueType.I32, true);
                comparisonHelper(Opcodes.IFGE);
            }
            case I32_GE_U -> {
                compare(ValueType.I32, false);
                comparisonHelper(Opcodes.IFGE);
            }

            // I64 Tests & Comparisons
            case I64_EQZ -> {
                code.visitLdcInsn(0);
                push(ValueType.I64);
                testIntEquality(ValueType.I64);
            }
            case I64_EQ -> testIntEquality(ValueType.I64);
            case I64_NE -> testIntInequality(ValueType.I64);
            case I64_LT_S -> {
                compare(ValueType.I64, true);
                comparisonHelper(Opcodes.IFLT);
            }
            case I64_LT_U -> {
                compare(ValueType.I64, false);
                comparisonHelper(Opcodes.IFLT);
            }
            case I64_GT_S -> {
                compare(ValueType.I64, true);
                comparisonHelper(Opcodes.IFGT);
            }
            case I64_GT_U -> {
                compare(ValueType.I64, false);
                comparisonHelper(Opcodes.IFGT);
            }
            case I64_LE_S -> {
                compare(ValueType.I64, true);
                comparisonHelper(Opcodes.IFLE);
            }
            case I64_LE_U -> {
                compare(ValueType.I64, false);
                comparisonHelper(Opcodes.IFLE);
            }
            case I64_GE_S -> {
                compare(ValueType.I64, true);
                comparisonHelper(Opcodes.IFGE);
            }
            case I64_GE_U -> {
                compare(ValueType.I64, false);
                comparisonHelper(Opcodes.IFGE);
            }

            // F32 Tests & Comparisons
            case F32_EQ -> {
                compare(ValueType.F32, false);
                comparisonHelper(Opcodes.IFEQ);
            }
            case F32_NE -> {
                compare(ValueType.F32, false);
                comparisonHelper(Opcodes.IFNE);
            }
            case F32_LT -> {
                compare(ValueType.F32, false);
                comparisonHelper(Opcodes.IFLT);
            }
            case F32_GT -> {
                compare(ValueType.F32, false);
                comparisonHelper(Opcodes.IFGT);
            }
            case F32_LE -> {
                compare(ValueType.F32, false);
                comparisonHelper(Opcodes.IFLE);
            }
            case F32_GE -> {
                compare(ValueType.F32, false);
                comparisonHelper(Opcodes.IFGE);
            }

            // F64 Tests & Comparisons
            case F64_EQ -> {
                compare(ValueType.F64, false);
                comparisonHelper(Opcodes.IFEQ);
            }
            case F64_NE -> {
                compare(ValueType.F64, false);
                comparisonHelper(Opcodes.IFNE);
            }
            case F64_LT -> {
                compare(ValueType.F64, false);
                comparisonHelper(Opcodes.IFLT);
            }
            case F64_GT -> {
                compare(ValueType.F64, false);
                comparisonHelper(Opcodes.IFGT);
            }
            case F64_LE -> {
                compare(ValueType.F64, false);
                comparisonHelper(Opcodes.IFLE);
            }
            case F64_GE -> {
                compare(ValueType.F64, false);
                comparisonHelper(Opcodes.IFGE);
            }

            // I32 Math
            case I32_CLZ -> clz(ValueType.I32);
            case I32_CTZ -> ctz(ValueType.I32);
            case I32_POPCNT -> popcnt(ValueType.I32);
            case I32_ADD -> add(ValueType.I32);
            case I32_SUB -> sub(ValueType.I32);
            case I32_MUL -> mul(ValueType.I32);
            case I32_DIV_S -> iDiv(ValueType.I32, true);
            case I32_DIV_U -> iDiv(ValueType.I32, false);
            case I32_REM_S -> iRem(ValueType.I32, true);
            case I32_REM_U -> iRem(ValueType.I32, false);
            case I32_AND -> iAnd(ValueType.I32);
            case I32_OR -> iOr(ValueType.I32);
            case I32_XOR -> iXor(ValueType.I32);
            case I32_SHL -> shl(ValueType.I32);
            case I32_SHR_S -> iShr(ValueType.I32, true);
            case I32_SHR_U -> iShr(ValueType.I32, false);
            case I32_ROTL -> iRotl(ValueType.I32);
            case I32_ROTR -> iRotr(ValueType.I32);

            // I64 Math
            case I64_CLZ -> clz(ValueType.I64);
            case I64_CTZ -> ctz(ValueType.I64);
            case I64_POPCNT -> popcnt(ValueType.I64);
            case I64_ADD -> add(ValueType.I64);
            case I64_SUB -> sub(ValueType.I64);
            case I64_MUL -> mul(ValueType.I64);
            case I64_DIV_S -> iDiv(ValueType.I64, true);
            case I64_DIV_U -> iDiv(ValueType.I64, false);
            case I64_REM_S -> iRem(ValueType.I64, true);
            case I64_REM_U -> iRem(ValueType.I64, false);
            case I64_AND -> iAnd(ValueType.I64);
            case I64_OR -> iOr(ValueType.I64);
            case I64_XOR -> iXor(ValueType.I64);
            case I64_SHL -> shl(ValueType.I64);
            case I64_SHR_S -> iShr(ValueType.I64, true);
            case I64_SHR_U -> iShr(ValueType.I64, false);
            case I64_ROTL -> iRotl(ValueType.I64);
            case I64_ROTR -> iRotr(ValueType.I64);

            // F32 Math
            case F32_ABS -> fAbs(ValueType.F32);
            case F32_NEG -> fNeg(ValueType.F32);
            case F32_CEIL -> fCeil(ValueType.F32);
            case F32_FLOOR -> fFloor(ValueType.F32);
            case F32_TRUNC -> fTrunc(ValueType.F32);
            case F32_NEAREST -> fNearest(ValueType.F32);
            case F32_SQRT -> fSqrt(ValueType.F32);
            case F32_ADD -> add(ValueType.F32);
            case F32_SUB -> sub(ValueType.F32);
            case F32_MUL -> mul(ValueType.F32);
            case F32_DIV -> fDiv(ValueType.F32);
            case F32_MIN -> fMin(ValueType.F32);
            case F32_MAX -> fMax(ValueType.F32);
            case F32_COPYSIGN -> fCopysign(ValueType.F32);

            // F64 Math
            case F64_ABS -> fAbs(ValueType.F64);
            case F64_NEG -> fNeg(ValueType.F64);
            case F64_CEIL -> fCeil(ValueType.F64);
            case F64_FLOOR -> fFloor(ValueType.F64);
            case F64_TRUNC -> fTrunc(ValueType.F64);
            case F64_NEAREST -> fNearest(ValueType.F64);
            case F64_SQRT -> fSqrt(ValueType.F64);
            case F64_ADD -> add(ValueType.F64);
            case F64_SUB -> sub(ValueType.F64);
            case F64_MUL -> mul(ValueType.F64);
            case F64_DIV -> fDiv(ValueType.F64);
            case F64_MIN -> fMin(ValueType.F64);
            case F64_MAX -> fMax(ValueType.F64);
            case F64_COPYSIGN -> fCopysign(ValueType.F64);

            // Numeric Conversions
            case I32_WRAP_I64 -> {

            }
            case I32_TRUNC_F32_S -> trunc(ValueType.I32, ValueType.F32, true);
            case I32_TRUNC_F32_U -> trunc(ValueType.I32, ValueType.F32, false);
            case I32_TRUNC_F64_S -> trunc(ValueType.I32, ValueType.F64, true);
            case I32_TRUNC_F64_U -> trunc(ValueType.I32, ValueType.F64, false);
            case I64_EXTEND_I32_S -> iExtend(true);
            case I64_EXTEND_I32_U -> iExtend(false);
            case I64_TRUNC_F32_S -> trunc(ValueType.I64, ValueType.F32, true);
            case I64_TRUNC_F32_U -> trunc(ValueType.I64, ValueType.F32, false);
            case I64_TRUNC_F64_S -> trunc(ValueType.I64, ValueType.F64, true);
            case I64_TRUNC_F64_U -> trunc(ValueType.I64, ValueType.F64, false);
            case F32_CONVERT_I32_S -> fConvert(ValueType.F32, ValueType.I32, true);
            case F32_CONVERT_I32_U -> fConvert(ValueType.F32, ValueType.I32, false);
            case F32_CONVERT_I64_S -> fConvert(ValueType.F32, ValueType.I64, true);
            case F32_CONVERT_I64_U -> fConvert(ValueType.F32, ValueType.I64, false);
            case F32_DEMOTE_F64 -> {

            }
            case F64_CONVERT_I32_S -> fConvert(ValueType.F64, ValueType.I32, true);
            case F64_CONVERT_I32_U -> fConvert(ValueType.F64, ValueType.I32, false);
            case F64_CONVERT_I64_S -> fConvert(ValueType.F64, ValueType.I64, true);
            case F64_CONVERT_I64_U -> fConvert(ValueType.F64, ValueType.I64, false);
            case F64_PROMOTE_F32 -> {

            }
            case I32_REINTERPRET_F32 -> reinterpret(ValueType.I32, ValueType.F32);
            case I64_REINTERPRET_F64 -> reinterpret(ValueType.I64, ValueType.F64);
            case F32_REINTERPRET_I32 -> reinterpret(ValueType.F32, ValueType.I32);
            case F64_REINTERPRET_I64 -> reinterpret(ValueType.F64, ValueType.I64);
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

    protected void compare(ValueType t, boolean signed){

        var method = signed ? "compare" : "compareUnsigned";

        switch (t){
            case I32 -> code.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getDescriptor(Integer.class),
                    method,
                    Type.getMethodDescriptor(Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE), false);

            case I64 -> code.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getDescriptor(Long.class),
                    method,
                    Type.getMethodDescriptor(Type.INT_TYPE, Type.LONG_TYPE, Type.LONG_TYPE), false);

            case F32 -> code.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getDescriptor(Float.class),
                    "compare",
                    Type.getMethodDescriptor(Type.INT_TYPE, Type.FLOAT_TYPE, Type.FLOAT_TYPE), false);

            case F64 -> code.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getDescriptor(Double.class),
                    "compare",
                    Type.getMethodDescriptor(Type.INT_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE), false);
        }

        pop();
        pop();
        push(ValueType.I32);

    }

    protected void comparisonHelper(int opcode){
        var trueBranch = new Label();
        var end = new Label();
        code.visitJumpInsn(opcode, trueBranch);
        code.visitLdcInsn(0);
        code.visitJumpInsn(Opcodes.GOTO, end);
        code.visitLabel(trueBranch);
        code.visitLdcInsn(1);
        code.visitLabel(end);

    }

    protected void clz(ValueType t){
        switch (t){
            case I32 -> {
                code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class),
                        "numberOfLeadingZeros",
                        Type.getMethodDescriptor(Type.INT_TYPE, Type.INT_TYPE), false);
            }
            case I64 -> {
                code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Long.class),
                        "numberOfLeadingZeros",
                        Type.getMethodDescriptor(Type.INT_TYPE, Type.LONG_TYPE), false);
                pop();
                push(ValueType.I32);
            }
        }
    }

    protected void ctz(ValueType t){
        switch (t){
            case I32 -> {
                code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class),
                        "numberOfTrailingZeros",
                        Type.getMethodDescriptor(Type.INT_TYPE, Type.INT_TYPE), false);
            }
            case I64 -> {
                code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Long.class),
                        "numberOfTrailingZeros",
                        Type.getMethodDescriptor(Type.INT_TYPE, Type.LONG_TYPE), false);
                pop();
                push(ValueType.I32);
            }
        }
    }

    protected void popcnt(ValueType t){
        switch (t){
            case I32 -> {
                code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class),
                        "bitCount",
                        Type.getMethodDescriptor(Type.INT_TYPE, Type.INT_TYPE), false);
            }
            case I64 -> {
                code.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Long.class),
                        "bitCount",
                        Type.getMethodDescriptor(Type.INT_TYPE, Type.LONG_TYPE), false);
                pop();
                push(ValueType.I32);
            }
        }
    }

    protected void add(ValueType t){
        switch (t){
            case I32 -> code.visitInsn(Opcodes.IADD);
            case I64 -> code.visitInsn(Opcodes.LADD);
            case F32 -> code.visitInsn(Opcodes.FADD);
            case F64 -> code.visitInsn(Opcodes.DADD);
        }

        pop();
        pop();
        push(t);
    }

    protected void sub(ValueType t){
        switch (t){
            case I32 -> code.visitInsn(Opcodes.ISUB);
            case I64 -> code.visitInsn(Opcodes.LSUB);
            case F32 -> code.visitInsn(Opcodes.FSUB);
            case F64 -> code.visitInsn(Opcodes.DSUB);
        }

        pop();
        pop();
        push(t);
    }

    protected void mul(ValueType t){
        switch (t){
            case I32 -> code.visitInsn(Opcodes.IMUL);
            case I64 -> code.visitInsn(Opcodes.LMUL);
            case F32 -> code.visitInsn(Opcodes.FMUL);
            case F64 -> code.visitInsn(Opcodes.DMUL);
        }

        pop();
        pop();
        push(t);
    }

    protected void iDiv(ValueType t, boolean signed){

    }

    protected void iRem(ValueType t, boolean signed){

    }

    protected void iAnd(ValueType t){

    }

    protected void iOr(ValueType t){

    }

    protected void iXor(ValueType t){

    }

    protected void shl(ValueType t){

    }

    protected void iShr(ValueType t, boolean signed){

    }

    protected void iRotl(ValueType t){

    }

    protected void iRotr(ValueType t){

    }

    protected void fAbs(ValueType t){

    }

    protected void fNeg(ValueType t){

    }

    protected void fCeil(ValueType t){

    }

    protected void fFloor(ValueType t){

    }

    protected void fTrunc(ValueType t){

    }

    protected void fNearest(ValueType t){

    }

    protected void fSqrt(ValueType t){

    }

    protected void fDiv(ValueType t){

    }

    protected void fMin(ValueType t){

    }

    protected void fMax(ValueType t){

    }

    protected void fCopysign(ValueType t){

    }

    protected void trunc(ValueType i, ValueType f, boolean signed){

    }

    protected void iExtend(boolean signed){

    }

    protected void fConvert(ValueType f, ValueType i, boolean signed){

    }

    protected void reinterpret(ValueType r, ValueType o){

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
