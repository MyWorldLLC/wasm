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
