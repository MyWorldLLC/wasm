package com.myworldvw.wasm.util;

/**
 * WebAssembly tends to use u32 types as indices into tables. This poses
 * a problem for us working in Java, since Java interprets all integers
 * as signed and since arrays can only be indexed with integers. To work
 * around this, we split the array of elements into 1-2 arrays, where
 * indices that have the sign bit (in a signed interpretation) set
 * map into the higher array and others map into a lower array.
 * @param <T>
 */
public class Table<T> {

    public static final long SPLIT_BIT_MASK = 0x00_00_00_00_10_00_00_00L;

    protected final Object[] lower;
    protected final Object[] upper;

    public Table(){
        lower = new Object[]{};
        upper = new Object[]{};
    }

    public Object get(long index){

        if((SPLIT_BIT_MASK & index) != 0){
            return upper[(int)(~SPLIT_BIT_MASK & index)];
        }else{
            return lower[(int)index];
        }
    }
}
