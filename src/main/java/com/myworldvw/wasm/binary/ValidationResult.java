package com.myworldvw.wasm.binary;

public record ValidationResult(String message, Object... fmtArgs) {
    public static final ValidationResult OK = null;

}
