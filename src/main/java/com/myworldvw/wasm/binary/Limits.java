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

public record Limits(int min, int max) implements Validatable {

    public static final int NO_MAX = -1;
    public static final int VALID_RANGE = 1 << 16;

    Limits(int min){
        this(min, NO_MAX);
    }

    public boolean hasMax(){
        return max != NO_MAX;
    }

    @Override
    public void validate() throws ValidationFailure {
        Require.satisfies(min, i -> i <= VALID_RANGE,
                "min (%d) must be within range %d", min, VALID_RANGE);
        if(hasMax()){
            Require.satisfies(max, i -> i <= VALID_RANGE, "max (%d) must be within range %d", max, VALID_RANGE);
            Require.satisfies(max, i -> i >= min, "max (%d) must be >= min (%d)", max, min);
        }
    }
}
