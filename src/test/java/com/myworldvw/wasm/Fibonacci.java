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

package com.myworldvw.wasm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Fibonacci {

    @Test
    void runsFibonacci() throws Throwable {
        var config = new WasmConfig();
        config.setCompiledModulePackage("foo.bar");

        var ctx = WasmContext.createFromResources(config,"/wasm/fibonacci.wasm");
        ctx.instantiate("fibonacci");
        var handle = ctx.getExportedFunction("fibonacci", "fib").get();

        assertEquals(5, (int) handle.invokeExact(5));
    }

}
