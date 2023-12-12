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

import java.lang.invoke.MethodHandles;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FunctionCallTests {
    @Test
    void callsInternalFunction() throws Throwable {
        var ctx = WasmContext.createFromResources("/wasm/callAFunction.wasm");
        ctx.instantiate("callAFunction");
        var handle = ctx.getExportedFunction("callAFunction", "callMe").get();

        assertEquals(3, (int) handle.invokeExact());
    }

    @Test
    void callsIndirectFunction() throws Throwable {
        var ctx = WasmContext.createFromResources("/wasm/callIndirectFunction.wasm");
        var module = ctx.instantiate("callIndirectFunction");
        module.getTable().set(0, MethodHandles.constant(int.class, 3));
        var handle = ctx.getExportedFunction("callIndirectFunction", "callMe").get();

        assertEquals(3, (int) handle.invokeExact());
    }

    @Test
    void callsImportedFunction() throws Throwable {
        var ctx = WasmContext.createFromResources("/wasm/callImportedFunction.wasm");
        var module = ctx.instantiate("callImportedFunction", new Imports()
                .function("env", "importMe", MethodHandles.constant(int.class, 3)));
        var handle = ctx.getExportedFunction("callImportedFunction", "callMe").get();

        assertEquals(3, (int) handle.invokeExact());
    }
}
