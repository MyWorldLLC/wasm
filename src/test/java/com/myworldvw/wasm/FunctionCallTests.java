package com.myworldvw.wasm;

import com.myworldvw.wasm.util.WasmLoader;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FunctionCallTests {
    @Test
    void callsInternalFunction() throws Throwable {
        var ctx = WasmLoader.createFromResources("/wasm/callAFunction.wasm");
        ctx.instantiate("callAFunction");
        var handle = ctx.getExportedFunction("callAFunction", "callMe").get();

        assertEquals(3, (int) handle.invokeExact());
    }

    @Test
    void callsIndirectFunction() throws Throwable {
        var ctx = WasmLoader.createFromResources("/wasm/callIndirectFunction.wasm");
        var module = ctx.instantiate("callIndirectFunction");
        module.getTable().set(0, MethodHandles.constant(int.class, 3));
        var handle = ctx.getExportedFunction("callIndirectFunction", "callMe").get();

        assertEquals(3, (int) handle.invokeExact());
    }
}
