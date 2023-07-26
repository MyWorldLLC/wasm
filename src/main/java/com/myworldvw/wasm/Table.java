package com.myworldvw.wasm;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Optional;

public class Table {

    protected MethodHandle[] entries;
    protected final Optional<Integer> maxSize;

    public Table(){
        this(Optional.empty());
    }

    public Table(Optional<Integer> maxSize){
        this.maxSize = maxSize;
    }

    public Optional<Integer> getMaxSize(){
        return maxSize;
    }

    public void set(int id, MethodHandle handle){
        ensureSize(id);
        entries[id] = handle;
    }

    public MethodHandle get(int id){
        ensureSize(id);
        return entries[id];
    }

    protected void ensureSize(int id){

        if(entries != null && id < entries.length){
            return;
        }

        if(maxSize.map(m -> id < m).orElse(true)){
            if(entries == null){
                entries = new MethodHandle[id];
            }else{
                entries = Arrays.copyOf(entries, id);
            }
        }else{
            throw new IllegalArgumentException("Cannot resize to accomodate %d: max size is %d".formatted(id, maxSize.get()));
        }
    }

}
