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
        // TODO - trap if out of accepted ranges
        f = (float) (f < 0 ? Math.ceil(f) : Math.floor(f));
        if(signed != 0){
            return Math.round(f);
        }

        // Only positive or zero floats are accepted as input for this op
        if(f > (float) Integer.MAX_VALUE){
            // This works for getting the unsigned value because binary addition
            // yields the correct interpretation in both unsigned & 2's-complement
            // no matter which the operands are considered to be.
            return Integer.MAX_VALUE + (int) (f - (float) Integer.MAX_VALUE);
        }
        return (int) f;
    }

    public static long truncateF32ToI64(float f, int signed){
        // TODO - trap if out of accepted ranges
        f = (float) (f < 0 ? Math.ceil(f) : Math.floor(f));
        if(signed != 0){
            return Math.round((double) f);
        }

        // Only positive or zero floats are accepted as input for this op
        if(f > (float) Long.MAX_VALUE){
            return Long.MAX_VALUE + (long) (f - (float) Long.MAX_VALUE);
        }
        return (long) f;
    }

    public static int truncateF64ToI32(double d, int signed){
        d = d < 0 ? Math.ceil(d) : Math.floor(d);
        if(signed != 0){
            return (int) Math.round(d);
        }

        // Only positive or zero floats are accepted as input for this op
        if(d > (double) Integer.MAX_VALUE){
            // This works for getting the unsigned value because binary addition
            // yields the correct interpretation in both unsigned & 2's-complement
            // no matter which the operands are considered to be.
            return Integer.MAX_VALUE + (int) (d - (double) Integer.MAX_VALUE);
        }
        return (int) d;
    }

    public static long truncateF64ToI64(double d, int signed){
        d = d < 0 ? Math.ceil(d) : Math.floor(d);
        if(signed != 0){
            return Math.round(d);
        }

        // Only positive or zero floats are accepted as input for this op
        if(d > (double) Long.MAX_VALUE){
            // This works for getting the unsigned value because binary addition
            // yields the correct interpretation in both unsigned & 2's-complement
            // no matter which the operands are considered to be.
            return Long.MAX_VALUE + (long) (d - (double) Long.MAX_VALUE);
        }
        return (long) d;
    }

    public static float convertI32ToF32(int i, int signed){
        if(signed != 0){
            return (float) i;
        }

        // Unsigned, inverse of the truncation and operating on the same
        // principle as above, but with binary subtraction instead of
        // addition.
        if(i < 0){ // Detect sign bit set, indicating that a plain cast
            // will not work here.
            return (float) (Integer.MAX_VALUE) + (float) (i - Integer.MAX_VALUE);
        }
        return (float) i;
    }

    public static double convertI32ToF64(int i, int signed){
        if(signed != 0){
            return (double) i;
        }

        // Unsigned, inverse of the truncation and operating on the same
        // principle as above, but with binary subtraction instead of
        // addition.
        if(i < 0){ // Detect sign bit set, indicating that a plain cast
            // will not work here.
            return (double) (Integer.MAX_VALUE) + (double) (i - Integer.MAX_VALUE);
        }
        return (double) i;
    }

    public static float convertI64ToF32(long l, int signed){
        if(signed != 0){
            return (float) l;
        }

        // Unsigned, inverse of the truncation and operating on the same
        // principle as above, but with binary subtraction instead of
        // addition.
        if(l < 0){ // Detect sign bit set, indicating that a plain cast
            // will not work here.
            return (float) (Long.MAX_VALUE) + (float) (l - Long.MAX_VALUE);
        }
        return (float) l;
    }

    public static double convertI64ToF64(long l, int signed){
        if(signed != 0){
            return (double) l;
        }

        // Unsigned, inverse of the truncation and operating on the same
        // principle as above, but with binary subtraction instead of
        // addition.
        if(l < 0){ // Detect sign bit set, indicating that a plain cast
            // will not work here.
            return (double) (Long.MAX_VALUE) + (double) (l - Long.MAX_VALUE);
        }
        return (double) l;
    }
}
