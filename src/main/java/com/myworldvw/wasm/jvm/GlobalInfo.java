package com.myworldvw.wasm.jvm;

import com.myworldvw.wasm.binary.GlobalType;

public record GlobalInfo(String module, String name, String fieldName, GlobalType type) {}
