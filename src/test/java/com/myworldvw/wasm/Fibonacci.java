package com.myworldvw.wasm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Fibonacci {

    @Test
    void runsFibonacci() throws Throwable {
        var config = new WasmContextConfig();
        config.setCompiledModulePackage("foo.bar");

        var ctx = WasmContext.createFromResources(config,"/wasm/fibonacci.wasm");
        ctx.instantiate("fibonacci");
        var handle = ctx.getExportedFunction("fibonacci", "fib").get();

        assertEquals(5, (int) handle.invokeExact(5));
    }

}
