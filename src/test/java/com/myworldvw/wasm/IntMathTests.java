package com.myworldvw.wasm;

import com.myworldvw.wasm.util.WasmLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntMathTests {

    @Test
    void addsIntegers() throws Throwable {
        var ctx = WasmLoader.createFromResources("/wasm/addToThree.wasm");
        ctx.instantiate("addToThree");
        var handle = ctx.getExportedFunction("addToThree", "add").get();

        assertEquals(3, (int) handle.invokeExact());
    }

    @Test
    void addsIntegerParams() throws Throwable {
        var ctx = WasmLoader.createFromResources("/wasm/addI32Params.wasm");
        ctx.instantiate("addI32Params");
        var handle = ctx.getExportedFunction("addI32Params", "add").get();

        assertEquals(7, (int) handle.invokeExact(4, 3));
    }

}
