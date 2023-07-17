package com.myworldvw.wasm.binary;

public class ValidationFailure extends Exception {

    protected final ValidationResult result;

    public ValidationFailure(ValidationResult result){
        super(String.format(result.message(), result.fmtArgs()));
        this.result = result;
    }

    public ValidationResult result(){
        return result;
    }
}
