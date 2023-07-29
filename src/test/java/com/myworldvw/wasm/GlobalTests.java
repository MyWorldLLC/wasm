package com.myworldvw.wasm;

import com.myworldvw.wasm.globals.I32Global;
import com.myworldvw.wasm.util.WasmLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GlobalTests {

    @Test
    void readsAndWritesLocalAndImportedGlobals() throws Throwable {
        var ctx = WasmLoader.createFromResources("/wasm/globalsI32.wasm");
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
        var ctx = WasmLoader.createFromResources("/wasm/globalInitializers.wasm");
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
