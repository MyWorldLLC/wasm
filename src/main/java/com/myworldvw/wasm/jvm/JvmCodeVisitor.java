package com.myworldvw.wasm.jvm;

import com.myworldvw.wasm.binary.CodeVisitor;
import com.myworldvw.wasm.binary.FunctionType;
import com.myworldvw.wasm.binary.ValueType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Optional;
import java.util.Stack;

import static com.myworldvw.wasm.binary.WasmOpcodes.*;

public class JvmCodeVisitor implements CodeVisitor {

    record BlockLabels(Label start, Label end){}

    protected final MethodVisitor code;
    protected FunctionType signature;
    protected final Stack<Optional<ValueType>> blockTypes;
    protected final Stack<BlockLabels> blockLabels;
    // TODO - need to track operands across block frames
    protected final Stack<ValueType> operands;
    protected ValueType[] locals;

    public JvmCodeVisitor(MethodVisitor code){
        this.code = code;
        blockTypes = new Stack<>();
        blockLabels = new Stack<>();
        operands = new Stack<>();
    }

    @Override
    public void visitLocals(ValueType[] locals) {
        this.locals = locals;
    }

    @Override
    public void exitBlock() {
        // Exiting an internal block
        var labels = blockLabels.pop();
        code.visitLabel(labels.end());
        // TODO - pop operands
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
        blockTypes.push(blockType);
        var start = new Label();
        code.visitLabel(start);
        blockLabels.push(new BlockLabels(start, new Label()));
    }

    @Override
    public void visitBranch(byte opcode, int labelId) {

    }

    @Override
    public void visitTableBranch(byte opcode, int[] labelIds, int defaultTarget) {

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
        switch (opcode) {
            case CALL -> {}
            case CALL_INDIRECT -> {}
        }
    }

    @Override
    public void visitParametric(byte opcode) {
        switch (opcode){
            case DROP -> {
                code.visitInsn(Opcodes.POP);
            }
            case SELECT -> {
                // TODO
            }
        }
    }

    @Override
    public void visitVar(byte opcode, int id) {
        var type = paramOrLocal(id);
        id = id + 1; // offset by 1 since l0 is always 'this'
        switch (opcode){
            case LOCAL_GET -> {
                push(type);
                switch (type){
                    // TODO - this doesn't account for longs/doubles taking up 2 local slots
                    case I32 -> code.visitVarInsn(Opcodes.ILOAD, id);
                    case F32 -> code.visitVarInsn(Opcodes.FLOAD, id);
                    case I64 -> code.visitVarInsn(Opcodes.LLOAD, id);
                    case F64 -> code.visitVarInsn(Opcodes.DLOAD, id);
                }
            }
            case LOCAL_SET -> {
                pop();
                switch (type){
                    // TODO - this doesn't account for longs/doubles taking up 2 local slots
                    case I32 -> code.visitVarInsn(Opcodes.ISTORE, id);
                    case F32 -> code.visitVarInsn(Opcodes.FSTORE, id);
                    case I64 -> code.visitVarInsn(Opcodes.LSTORE, id);
                    case F64 -> code.visitVarInsn(Opcodes.DSTORE, id);
                }
            }
            case LOCAL_TEE -> {
                code.visitInsn(Opcodes.DUP); // TODO - this is probably wrong with long/double
                switch (type){
                    // TODO - this doesn't account for longs/doubles taking up 2 local slots
                    case I32 -> code.visitVarInsn(Opcodes.ISTORE, id);
                    case F32 -> code.visitVarInsn(Opcodes.FSTORE, id);
                    case I64 -> code.visitVarInsn(Opcodes.LSTORE, id);
                    case F64 -> code.visitVarInsn(Opcodes.DSTORE, id);
                }
            }
            case GLOBAL_GET, GLOBAL_SET -> {
                // TODO
            }
        }
    }

    @Override
    public void visitMemory(byte opcode, int align, int offset) {

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
        signature.returnType().ifPresentOrElse(
                t -> {
                    switch (t) {
                        case I32 -> code.visitInsn(Opcodes.IRETURN);
                        case F32 -> code.visitInsn(Opcodes.FRETURN);
                        case I64 -> code.visitInsn(Opcodes.LRETURN);
                        case F64 -> code.visitInsn(Opcodes.DRETURN);
                    }
                }, () -> code.visitInsn(Opcodes.RETURN));
    }

    protected ValueType paramOrLocal(int id){
        if(id < signature.params().length){
            return signature.params()[id];
        }
        return locals[id];
    }

    protected void push(ValueType t){
        operands.push(t);
    }

    protected ValueType pop(){
        return operands.pop();
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
}
