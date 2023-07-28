package com.myworldvw.wasm.binary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class WasmBinaryModule {

    protected final String name;

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

    public WasmBinaryModule(String name){
        this.name = name;
        customSections = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<CustomSection> getCustomSections() {
        return customSections;
    }

    public FunctionType[] getTypeSection() {
        return typeSection;
    }

    public Import[] getImportSection() {
        return importSection;
    }

    public TypeId[] getFunctionSection() {
        return functionSection;
    }

    public TableType[] getTableSection() {
        return tableSection;
    }

    public MemoryType[] getMemorySection() {
        return memorySection;
    }

    public byte[] getGlobalSection() {
        return globalSection;
    }

    public Export[] getExportSection() {
        return exportSection;
    }

    public FunctionId getStart() {
        return start;
    }

    public byte[] getElementSection() {
        return elementSection;
    }

    public Code[] getCodeSection() {
        return codeSection;
    }

    public byte[] getDataSection() {
        return dataSection;
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

    public FunctionType typeForFunction(FunctionId function){
        return typeSection[functionSection[function.id()].id()];
    }

    public boolean isExported(FunctionId function){
        return exportSection != null && Arrays.stream(exportSection)
                .anyMatch(e -> e.descriptor().type() == ExportDescriptor.Type.FUNCTION_ID
                        && e.descriptor().functionId().equals(function));
    }

    public Optional<String> getExportedName(FunctionId function){
        if(exportSection == null){
            return Optional.empty();
        }

        return Arrays.stream(exportSection)
                .filter(e -> e.descriptor().type() == ExportDescriptor.Type.FUNCTION_ID
                    && e.descriptor().functionId().equals(function))
                .map(Export::name)
                .findFirst();
    }
}
