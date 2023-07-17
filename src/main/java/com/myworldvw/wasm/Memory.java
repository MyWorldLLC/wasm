package com.myworldvw.wasm;

import com.myworldvw.wasm.binary.Limits;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Memory {

    enum AllocationMode {
        ON_DEMAND, IMMEDIATE
    }

    public static final int PAGE_SIZE = 65536;

    protected ByteBuffer storage;
    protected int pages;
    protected Limits limits;
    protected AllocationMode allocationMode;

    public Memory(){
        allocationMode = AllocationMode.ON_DEMAND;
        storage = ByteBuffer.allocate(0);
        storage.order(ByteOrder.LITTLE_ENDIAN);
    }

    public int readI32(int addr){
        try{
            return storage.getInt(addr);
        }catch (IndexOutOfBoundsException e){
            reallocate(addr);
            return storage.getInt(addr);
        }
    }

    public void writeI32(int addr, int value){
        try{
            storage.putInt(addr, value);
        }catch (IndexOutOfBoundsException e){
            reallocate(addr);
            storage.putInt(addr, value);
        }
    }

    public long readI64(int addr){
        try{
            return storage.getLong(addr);
        }catch (IndexOutOfBoundsException e){
            reallocate(addr);
            return storage.getLong(addr);
        }
    }

    public void writeI64(int addr, long value){
        try{
            storage.putLong(addr, value);
        }catch (IndexOutOfBoundsException e){
            reallocate(addr);
            storage.putLong(addr, value);
        }
    }

    public float readF32(int addr){
        try{
            return storage.getFloat(addr);
        }catch (IndexOutOfBoundsException e){
            reallocate(addr);
            return storage.getFloat(addr);
        }
    }

    public void writeF32(int addr, float value){
        try{
            storage.putFloat(addr, value);
        }catch (IndexOutOfBoundsException e){
            reallocate(addr);
            storage.putFloat(addr, value);
        }
    }

    public double readF64(int addr){
        try{
            return storage.getDouble(addr);
        }catch (IndexOutOfBoundsException e){
            reallocate(addr);
            return storage.getDouble(addr);
        }
    }

    public void writeF64(int addr, double value){
        try{
            storage.putDouble(addr, value);
        }catch (IndexOutOfBoundsException e){
            reallocate(addr);
            storage.putDouble(addr, value);
        }
    }

    protected void reallocate(int addr){
        // TODO - reallocate memory to a size that accommodates this address,
        // if the limits/allocation mode allow it
    }
}
