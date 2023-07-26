package com.myworldvw.wasm;

import com.myworldvw.wasm.binary.Import;
import com.myworldvw.wasm.binary.ImportDescriptor;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Optional;

public abstract class WasmModule {

    protected final String name;
    protected final Memory memory0;
    protected final Table table0;
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

    public Memory getMemory(){
        return memory0;
    }

    public Table getTable(){
        return table0;
    }

    public Import[] getImports(){
        return imports;
    }

    public Optional<MethodHandle> getImported(String module, String name){
        var imp = Arrays.stream(imports)
                .filter(i -> i.module().equals(module)
                        && i.name().equals(name)
                        && i.descriptor().type() == ImportDescriptor.Type.TYPE_ID)
                .findFirst();

        // TODO - verify that type id corresponds to imported id in table
        return imp.map(i -> table0.get(i.descriptor().typeId().id()));
    }

}
