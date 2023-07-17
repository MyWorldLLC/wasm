package com.myworldvw.wasm.binary;

import com.myworldvw.wasm.util.Require;

public record Limits(int min, int max) implements Validatable {

    public static final int NO_MAX = -1;
    public static final int VALID_RANGE = 1 << 16;

    Limits(int min){
        this(min, NO_MAX);
    }

    public boolean hasMax(){
        return max != NO_MAX;
    }

    @Override
    public void validate() throws ValidationFailure {
        Require.satisfies(min, i -> i <= VALID_RANGE,
                "min (%d) must be within range %d", min, VALID_RANGE);
        if(hasMax()){
            Require.satisfies(max, i -> i <= VALID_RANGE, "max (%d) must be within range %d", max, VALID_RANGE);
            Require.satisfies(max, i -> i >= min, "max (%d) must be >= min (%d)", max, min);
        }
    }
}
