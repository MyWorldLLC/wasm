package com.myworldvw.wasm;

import com.myworldvw.wasm.binary.Limits;

import java.lang.foreign.*;
import java.nio.ByteOrder;

public class Memory {

    enum AllocationMode {
        ON_DEMAND, IMMEDIATE
    }

    public static final ValueLayout.OfByte WASM_I8 = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.LITTLE_ENDIAN);
    public static final ValueLayout.OfShort WASM_I16 = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    public static final ValueLayout.OfInt WASM_I32 = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    public static final ValueLayout.OfLong WASM_I64 = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    public static final ValueLayout.OfFloat WASM_F32 = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    public static final ValueLayout.OfDouble WASM_F64 = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

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

    public void memset(byte value){
        memory.fill(value);
    }

    public void memset(int addr, byte value){
        memory.asSlice(addr).fill(value);
    }

    public void memset(int addr, long length, byte value){
        memory.asSlice(addr, length).fill(value);
    }

    public byte readI8(int addr){
        try{
            return memory.get(WASM_I8, addr);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            return memory.get(WASM_I8, addr);
        }
    }

    public void writeI8(int addr, short value){
        try{
            memory.setAtIndex(WASM_I16, addr, value);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            memory.setAtIndex(WASM_I16, addr, value);
        }
    }

    public short readI16(int addr){
        try{
            return memory.getAtIndex(WASM_I16, addr);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            return memory.getAtIndex(WASM_I16, addr);
        }
    }

    public void writeI16(int addr, short value){
        try{
            memory.setAtIndex(WASM_I16, addr, value);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            memory.setAtIndex(WASM_I16, addr, value);
        }
    }

    public int readI32(int addr){
        try{
            return memory.getAtIndex(WASM_I32, addr);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            return memory.getAtIndex(WASM_I32, addr);
        }
    }

    public void writeI32(int addr, int value){
        try{
            memory.setAtIndex(WASM_I32, addr, value);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            memory.setAtIndex(WASM_I32, addr, value);
        }
    }

    public long readI64(int addr){
        try{
            return memory.getAtIndex(WASM_I64, addr);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            return memory.getAtIndex(WASM_I64, addr);
        }
    }

    public void writeI64(int addr, long value){
        try{
            memory.setAtIndex(WASM_I64, addr, value);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            memory.setAtIndex(WASM_I64, addr, value);
        }
    }

    public float readF32(int addr){
        try{
            return memory.getAtIndex(WASM_F32, addr);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            return memory.getAtIndex(WASM_F32, addr);
        }
    }

    public void writeF32(int addr, float value){
        try{
            memory.setAtIndex(WASM_F32, addr, value);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            memory.setAtIndex(WASM_F32, addr, value);
        }
    }

    public double readF64(int addr){
        try{
            return memory.getAtIndex(WASM_F64, addr);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            return memory.getAtIndex(WASM_F64, addr);
        }
    }

    public void writeF64(int addr, double value){
        try{
            memory.setAtIndex(WASM_F64, addr, value);
        }catch (IndexOutOfBoundsException e){
            if(limits != null && limits.hasMax() && addr > limits.max() * PAGE_SIZE){
                throw new SegmentationException("Address 0x032%X is out of bounds (max 0x032%X)".formatted(addr, limits.max() * PAGE_SIZE));
            }
            reallocate(addr);
            memory.setAtIndex(WASM_F64, addr, value);
        }
    }

    protected synchronized void reallocate(int addr){
        // reallocate memory to a size that accommodates this address,
        // if the limits/allocation mode allow it

        var allocSize = allocationMode == AllocationMode.IMMEDIATE
                ? (long) pages * PAGE_SIZE
                : calculateOnDemandAllocation(addr);

        if(growBytes(allocSize) == -1){
            throw new SegmentationException("Cannot expand allocated memory: requested %d, maximum limit is %d".formatted(allocSize, limits.max() * PAGE_SIZE));
        }
    }

    public int size(){
        return pages;
    }

    public int grow(int newPages){
        return growBytes((long) newPages * PAGE_SIZE);
    }

    public synchronized int growBytes(long byteSize){
        if(limits != null){
            if(limits.hasMax() && limits.max() < byteSize * PAGE_SIZE){
                return -1;
            }
            byteSize = Math.max(byteSize, (long) limits.min() * PAGE_SIZE);
        }

        var newAllocator = Arena.openShared();
        var newMemory = newAllocator.allocate(byteSize, alignment);
        newMemory.fill((byte)0);

        if(memory != null){
            newMemory.copyFrom(memory);
        }

        if(allocator != null){
            allocator.close();
        }

        allocator = newAllocator;
        memory = newMemory;

        var oldPages = pages;
        pages = (int) (byteSize / PAGE_SIZE + 1);

        return oldPages;
    }

    protected long calculateOnDemandAllocation(int addr){
        long count = addr / paddingSize + 1; // number of "paddingSize" segments we must have
        return count * paddingSize;
    }

    public static void staticWriteI8(int baseAddr, byte value, Memory mem, int offsetAddr){
        mem.writeI8(baseAddr + offsetAddr, value);
    }

    public static void staticWriteI16(int baseAddr, short value, Memory mem, int offsetAddr){
        mem.writeI16(baseAddr + offsetAddr, value);
    }

    public static void staticWriteI32(int baseAddr, int value, Memory mem, int offsetAddr){
        mem.writeI32(baseAddr + offsetAddr, value);
    }

    public static void staticWriteI64(int baseAddr, long value, Memory mem, int offsetAddr){
        mem.writeI64(baseAddr + offsetAddr, value);
    }

    public static void staticWriteF32(int baseAddr, float value, Memory mem, int offsetAddr){
        mem.writeF32(baseAddr + offsetAddr, value);
    }

    public static void staticWriteF64(int baseAddr, double value, Memory mem, int offsetAddr){
        mem.writeF64(baseAddr + offsetAddr, value);
    }
}
