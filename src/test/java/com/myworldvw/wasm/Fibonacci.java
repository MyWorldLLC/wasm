package com.myworldvw.wasm;

import com.myworldvw.wasm.util.WasmLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Fibonacci {

    @Test
    void runsFibonacci() throws Throwable {
        var ctx = WasmLoader.createFromResources("/wasm/fibonacci.wasm");
        ctx.instantiate("fibonacci");
        var handle = ctx.getExportedFunction("fibonacci", "fib").get();

        assertEquals(3, (int) handle.invokeExact(5));
    }

}
