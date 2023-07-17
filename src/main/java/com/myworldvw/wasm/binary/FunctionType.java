package com.myworldvw.wasm.binary;

import com.myworldvw.wasm.util.Require;

import java.util.Arrays;

public record FunctionType(ValueType[] params, ValueType[] results) implements Validatable {
    @Override
    public void validate() throws ValidationFailure {
        Require.satisfies(results, r -> r == null || r.length <= 1,
                "Function result type must specify zero or one results: %s", Arrays.toString(results));
    }
}
