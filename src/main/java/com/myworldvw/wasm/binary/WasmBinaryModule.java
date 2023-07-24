package com.myworldvw.wasm.binary;

import java.util.ArrayList;
import java.util.List;

public class WasmBinaryModule {

    protected String name;

    protected final List<CustomSection> customSections;
    protected FunctionType[] typeSection;
    protected Import[] importSection;
    protected TypeId[] functionSection;
    protected TableType[] tableSection;
    protected MemoryType[] memorySection;
    protected byte[] globalSection;
    protected Export[] exportSection;
    protected FunctionId start;
    protected byte[] elementSection;
    protected Code[] codeSection;
    protected byte[] dataSection;

    public WasmBinaryModule(){
        customSections = new ArrayList<>();
    }

    public void addCustomSection(CustomSection section){
        customSections.add(section);
    }

    public void setTypeSection(FunctionType[] section){
        typeSection = section;
    }

    public void setImportSection(Import[] section){
        importSection = section;
    }

    public void setFunctionSection(TypeId[] section){
        functionSection = section;
    }

    public void setTableSection(TableType[] section){
        tableSection = section;
    }

    public void setMemorySection(MemoryType[] section){
        memorySection = section;
    }

    public void setGlobalSection(byte[] section){
        globalSection = section;
    }

    public void setExportSection(Export[] section){
        exportSection = section;
    }

    public void setStart(FunctionId start){
        this.start = start;
    }

    public void setElementSection(byte[] section){
        elementSection = section;
    }

    public void setCodeSection(Code[] section){
        codeSection = section;
    }

    public void setDataSection(byte[] section){
        dataSection = section;
    }
}
