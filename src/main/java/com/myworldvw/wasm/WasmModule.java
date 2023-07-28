package com.myworldvw.wasm;

import com.myworldvw.wasm.binary.Import;

public abstract class WasmModule {

    protected final String name;
    protected volatile boolean locked;
    protected volatile Memory memory0;
    protected volatile Table table0;
    protected final Import[] imports;

    public WasmModule(String name, Import[] imports){
        this.name = name;
        memory0 = new Memory();
        table0 = new Table();
        this.imports = imports;
    }

    public String getName(){
        return name;
    }

    public void importMemory(Memory memory){
        if(locked){
            throw new IllegalStateException("Module %s has already been initialized".formatted(name));
        }
        memory0 = memory;
    }

    public Memory getMemory(){
        return memory0;
    }

    public void importTable(Table table){
        if(locked){
            throw new IllegalStateException("Module %s has already been initialized".formatted(name));
        }
        table0 = table;
    }

    public Table getTable(){
        return table0;
    }

    public Import[] getImports(){
        return imports;
    }

    public void initializationComplete(){
        locked = true;
    }

}
