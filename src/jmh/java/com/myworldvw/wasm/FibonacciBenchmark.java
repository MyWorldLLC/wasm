package com.myworldvw.wasm;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

public class FibonacciBenchmark {

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @Fork(value = 1, warmups = 1, jvmArgsAppend = {"--enable-preview"})
    public void fibonacciWasm(FibonacciState state, Blackhole blackhole) throws Throwable {
        blackhole.consume((int) state.fibHandle.invokeExact(1000000));
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @Fork(value = 1, warmups = 1)
    public void fibonacciJava(Blackhole blackhole){
        // Implement in Java the same algorithm run by the wasm function
        // - the fibonacci demo found at https://www.assemblyscript.org/
        // (accessed 8/1/2023)
        var n = 1000000;

        var a = 0;
        var b = 1;
        if(n > 0){
            while(--n > 0){
                var t = a + b;
                a = b;
                b = t;
            }
            blackhole.consume(b);
        }
        blackhole.consume(a);
    }

}
