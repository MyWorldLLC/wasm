package com.myworldvw.wasm.jvm;

public class Runtime {

    public static int select(int a, int b, int test){
        return test != 0 ? a : b;
    }

    public static long select(long a, long b, int test){
        return test != 0 ? a : b;
    }

    public static float select(float a, float b, int test){
        return test != 0 ? a : b;
    }

    public static double select(double a, double b, int test){
        return test != 0 ? a : b;
    }

}
