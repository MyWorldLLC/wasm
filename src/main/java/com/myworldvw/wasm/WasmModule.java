package com.myworldvw.wasm;

import com.myworldvw.wasm.binary.sections.CustomSection;

import java.util.ArrayList;
import java.util.List;

public class WasmModule {

    protected String name;

    protected final List<CustomSection> customSections;
    protected long start;

    public WasmModule(){
        customSections = new ArrayList<>();
    }

    public void addCustomSection(CustomSection section){
        customSections.add(section);
    }
}
