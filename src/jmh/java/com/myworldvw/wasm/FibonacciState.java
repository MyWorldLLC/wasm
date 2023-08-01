package com.myworldvw.wasm;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.lang.invoke.MethodHandle;

@State(Scope.Thread)
public class FibonacciState {

    public WasmContext ctx;
    public WasmModule fibModule;
    public MethodHandle fibHandle;

    @Setup(Level.Trial)
    public void init() throws Exception {
        var config = new WasmConfig();
        config.setCompiledModulePackage("com.myworldvw.wasm.benchmark");

        ctx = WasmContext.createFromResources("/wasm/fibonacci.wasm");
        fibModule = ctx.instantiate("fibonacci");
        fibHandle = ctx.getExportedFunction("fibonacci", "fib").get();
    }
}
