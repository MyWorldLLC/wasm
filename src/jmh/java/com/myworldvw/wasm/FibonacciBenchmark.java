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
