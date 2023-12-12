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
