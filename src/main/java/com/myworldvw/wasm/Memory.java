package com.myworldvw.wasm;

import com.myworldvw.wasm.binary.Limits;

import java.lang.foreign.*;
import java.nio.ByteOrder;

public class Memory {

    enum AllocationMode {
        ON_DEMAND, IMMEDIATE
    }

    public static final ValueLayout.OfInt WASM_I32 = ValueLayout.JAVA_INT.withOrder(ByteOrder.LITTLE_ENDIAN);
    public static final ValueLayout.OfLong WASM_I64 = ValueLayout.JAVA_LONG.withOrder(ByteOrder.LITTLE_ENDIAN);
    public static final ValueLayout.OfFloat WASM_F32 = ValueLayout.JAVA_FLOAT.withOrder(ByteOrder.LITTLE_ENDIAN);
    public static final ValueLayout.OfDouble WASM_F64 = ValueLayout.JAVA_DOUBLE.withOrder(ByteOrder.LITTLE_ENDIAN);

    public static final int PAGE_SIZE = 65536;
    public static final int DEFAULT_PADDING_SIZE = 1024;
    public static final int DEFAULT_ALIGNMENT = 64;

    protected int pages;
    protected long paddingSize;
    protected int alignment;
    protected Limits limits;
    protected AllocationMode allocationMode;
    protected volatile Arena allocator;
    protected volatile MemorySegment memory;

    public Memory(){
        allocationMode = AllocationMode.ON_DEMAND;
        paddingSize = DEFAULT_PADDING_SIZE;
        alignment = DEFAULT_ALIGNMENT;
    }

    public long getPaddingSize(){
        return paddingSize;
    }

    public void setPaddingSize(long paddingSize){
        this.paddingSize = paddingSize;
    }

    public int getAlignmentConstraint(){
        return alignment;
    }

    public void setAlignmentConstraint(int alignment){
        this.alignment = alignment;
    }

    public int readI32(int addr){
        try{
            return memory.get(WASM_I32, addr);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            return memory.get(WASM_I32, addr);
        }
    }

    public void writeI32(int addr, int value){
        try{
            memory.set(WASM_I32, addr, value);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            memory.set(WASM_I32, addr, value);
        }
    }

    public long readI64(int addr){
        try{
            return memory.get(WASM_I64, addr);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            return memory.get(WASM_I64, addr);
        }
    }

    public void writeI64(int addr, long value){
        try{
            memory.set(WASM_I64, addr, value);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            memory.set(WASM_I64, addr, value);
        }
    }

    public float readF32(int addr){
        try{
            return memory.get(WASM_F32, addr);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            return memory.get(WASM_F32, addr);
        }
    }

    public void writeF32(int addr, float value){
        try{
            memory.set(WASM_F32, addr, value);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            memory.set(WASM_F32, addr, value);
        }
    }

    public double readF64(int addr){
        try{
            return memory.get(WASM_F64, addr);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            return memory.get(WASM_F64, addr);
        }
    }

    public void writeF64(int addr, double value){
        try{
            memory.set(WASM_F64, addr, value);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            memory.set(WASM_F64, addr, value);
        }
    }

    protected synchronized void reallocate(int addr){
        // reallocate memory to a size that accommodates this address,
        // if the limits/allocation mode allow it

        var allocSize = allocationMode == AllocationMode.IMMEDIATE
                ? (long) pages * PAGE_SIZE
                : calculateOnDemandAllocation(addr);

        if(limits != null){
            if(limits.hasMax() && limits.max() < allocSize){
                throw new SegmentationException("Cannot expand allocated memory: requested %d, maximum limit is %d".formatted(allocSize, limits.max()));
            }
            allocSize = Math.max(allocSize, limits.min());
        }

        var newAllocator = Arena.openShared();
        var newMemory = newAllocator.allocate(allocSize, alignment);

        if(memory != null){
            newMemory.copyFrom(memory);
        }

        if(allocator != null){
            allocator.close();
        }

        allocator = newAllocator;
        memory = newMemory;
    }

    protected long calculateOnDemandAllocation(int addr){
        long count = addr / paddingSize + 1; // number of "paddingSize" segments we must have
        return count * paddingSize;
    }
}
