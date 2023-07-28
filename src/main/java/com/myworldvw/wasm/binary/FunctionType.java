package com.myworldvw.wasm.binary;

import com.myworldvw.wasm.util.Require;

import java.util.Arrays;
import java.util.Optional;

public record FunctionType(ValueType[] params, ValueType[] results) implements Validatable {

    public boolean isVoid(){
        return results == null || results.length == 0;
    }

    public Optional<ValueType> returnType(){
        return isVoid() ? Optional.empty() : Optional.of(results[0]);
    }

    public FunctionType withTrailingParam(ValueType p){
        var newParams = Arrays.copyOf(params, params.length + 1);
        newParams[newParams.length - 1] = p;
        return new FunctionType(newParams, results);
    }

    @Override
    public void validate() throws ValidationFailure {
        Require.satisfies(results, r -> r == null || r.length <= 1,
                "Function result type must specify zero or one results: %s", Arrays.toString(results));
    }
}
