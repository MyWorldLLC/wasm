package com.myworldvw.wasm.jvm;

import com.myworldvw.wasm.binary.FunctionType;

public record FunctionInfo(String module, String name, FunctionType type, boolean imported, boolean exported) {}
