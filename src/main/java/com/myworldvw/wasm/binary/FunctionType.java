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

    @Override
    public void validate() throws ValidationFailure {
        Require.satisfies(results, r -> r == null || r.length <= 1,
                "Function result type must specify zero or one results: %s", Arrays.toString(results));
    }
}
