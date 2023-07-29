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

    public static int truncateF32ToI32(float f, int signed){
        f = (float) (f < 0 ? Math.ceil(f) : Math.floor(f));
        if(signed != 0){
            return Math.round(f);
        }
        return 0; // TODO
    }

    public static long truncateF32ToI64(float f, int signed){
        f = (float) (f < 0 ? Math.ceil(f) : Math.floor(f));
        if(signed != 0){
            return Math.round((double)f);
        }
        return 0; // TODO
    }

    public static int truncateF64ToI32(double d, int signed){
        d = d < 0 ? Math.ceil(d) : Math.floor(d);
        if(signed != 0){
            return (int) Math.round(d);
        }
        return 0; // TODO
    }

    public static long truncateF64ToI64(double d, int signed){
        d = d < 0 ? Math.ceil(d) : Math.floor(d);
        if(signed != 0){
            return Math.round(d);
        }
        return 0; // TODO
    }

    public static float convertI32ToF32(int i, int signed){
        return 0f; // TODO
    }

    public static double convertI32ToF64(int i, int signed){
        return 0d; // TODO
    }

    public static float convertI64ToF32(long l, int signed){
        return 0f; // TODO
    }

    public static double convertI64ToF64(long l, int signed){
        return 0d; // TODO
    }
}
