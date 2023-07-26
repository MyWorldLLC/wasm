package com.myworldvw.wasm;

import com.myworldvw.wasm.util.WasmLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntMathTests {

    @Test
    void addsIntegers() throws Throwable {
        var ctx = WasmLoader.createFromResources("/wasm/addToThree.wasm");
        ctx.instantiate("demo");
        var handle = ctx.getExportedFunction("demo", "add").get();

        assertEquals(3, (int) handle.invokeExact());
    }

}
