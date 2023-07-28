package com.myworldvw.wasm;

public class MissingImportException extends Exception {
    public MissingImportException(String module, String name, String type){
        super("Required import is missing: %s %s (%s)".formatted(module, name, type));
    }
}
