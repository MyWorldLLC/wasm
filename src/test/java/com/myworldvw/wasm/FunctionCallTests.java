package com.myworldvw.wasm;

import com.myworldvw.wasm.util.WasmLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FunctionCallTests {
    @Test
    void callsInternalFunction() throws Throwable {
        var ctx = WasmLoader.createFromResources("/wasm/callAFunction.wasm");
        ctx.instantiate("callAFunction");
        var handle = ctx.getExportedFunction("callAFunction", "callMe").get();

        assertEquals(3, (int) handle.invokeExact());
    }
}
