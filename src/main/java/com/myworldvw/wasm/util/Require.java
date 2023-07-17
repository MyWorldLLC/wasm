package com.myworldvw.wasm.util;

import com.myworldvw.wasm.binary.ValidationFailure;
import com.myworldvw.wasm.binary.ValidationResult;

import java.util.function.Predicate;

public class Require {

    public static <T> boolean test(T value, Predicate<T> condition){
        return condition.test(value);
    }

    public static <T> void satisfies(T value, Predicate<T> condition, String msg, Object... fmtArgs) throws ValidationFailure {
        if(!condition.test(value)){
            throw new ValidationFailure(new ValidationResult(msg, fmtArgs));
        }
    }

    public static void zeroOrGreater(int a, String msg, Object... fmtArgs) throws ValidationFailure {
        satisfies(a, i -> i > 0, msg);
    }
}
