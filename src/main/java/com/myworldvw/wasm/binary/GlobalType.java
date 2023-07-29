package com.myworldvw.wasm.binary;

import com.myworldvw.wasm.globals.Mutability;

public record GlobalType(ValueType valueType, Mutability mutability) {}
