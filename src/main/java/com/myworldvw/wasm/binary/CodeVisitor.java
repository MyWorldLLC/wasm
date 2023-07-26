package com.myworldvw.wasm.binary;

import java.util.Optional;

public interface CodeVisitor {

    void visitLocals(ValueType[] locals);
    void exitBlock();
    void exitFunction();
    void visitFunction(FunctionType type);
    void visitBlock(byte opcode, Optional<ValueType> blockType);
    void visitBranch(byte opcode, int labelId);
    void visitTableBranch(byte opcode, int[] labelIds, int defaultTarget);
    void visitCtrl(byte opcode);
    void visitCall(byte opcode, int target);
    void visitParametric(byte opcode);
    void visitVar(byte opcode, int id);
    void visitMemory(byte opcode, int align, int offset);
    void visitConst(byte opcode, long immediate);
    void visitNumeric(byte opcode);

}
