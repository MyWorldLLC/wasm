/*
 * Copyright 2023. MyWorld, LLC
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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

    public abstract void initialize();

}
