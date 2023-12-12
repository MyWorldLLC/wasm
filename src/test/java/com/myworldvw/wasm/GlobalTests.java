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

import com.myworldvw.wasm.globals.I32Global;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GlobalTests {

    @Test
    void readsAndWritesLocalAndImportedGlobals() throws Throwable {
        var ctx = WasmContext.createFromResources("/wasm/globalsI32.wasm");
        ctx.instantiate("globalsI32", new Imports()
                .global("env", "importMe", I32Global.mutable(2)));
        var callHandle = ctx.getExportedFunction("globalsI32", "callMe").get();
        var global = ctx.getExportedGlobal("globalsI32", "exportMe");

        assertEquals(5, (int) callHandle.invokeExact());
        assertTrue(global.isPresent());
        assertTrue(global.get() instanceof I32Global);
        assertEquals(3, ((I32Global) global.get()).getValue());
    }

    @Test
    void correctlyInitializesGlobals() throws Throwable {
        var ctx = WasmContext.createFromResources("/wasm/globalInitializers.wasm");
        ctx.instantiate("globalInitializers", new Imports()
                .global("env", "importMe", I32Global.immutable(2)));
        var global1 = ctx.getExportedGlobal("globalInitializers", "importMe");
        var global2 = ctx.getExportedGlobal("globalInitializers", "exportMe");

        assertTrue(global1.isPresent());
        assertTrue(global2.isPresent());
        assertEquals(2, ((I32Global) global1.get()).getValue());
        assertEquals(2, ((I32Global) global2.get()).getValue());
    }

}
