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

import com.myworldvw.wasm.globals.Global;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Imports {
    protected final List<SuppliedImport> imports;

    public Imports(){
        imports = new ArrayList<>();
    }

    public Imports function(String module, String name, MethodHandle function){
        imports.add(new SuppliedImport(module, name, function));
        return this;
    }

    public MethodHandle getFunction(String module, String name) throws MissingImportException {
        return getImported(module, name, SuppliedImport::isFunction)
                .map(p -> (MethodHandle) p)
                .orElseThrow(() -> new MissingImportException(module, name, "function"));
    }

    public Imports memory(String module, String name, Memory memory){
        imports.add(new SuppliedImport(module, name, memory));
        return this;
    }

    public Memory getMemory(String module, String name) throws MissingImportException {
        return getImported(module, name, SuppliedImport::isMemory)
                .map(p -> (Memory) p)
                .orElseThrow(() -> new MissingImportException(module, name, "memory"));
    }

    public Imports table(String module, String name, Table table){
        imports.add(new SuppliedImport(module, name, table));
        return this;
    }

    public Table getTable(String module, String name) throws MissingImportException {
        return getImported(module, name, SuppliedImport::isTable)
                .map(p -> (Table) p)
                .orElseThrow(() -> new MissingImportException(module, name, "table"));
    }

    public <T extends Global<?>> Imports global(String module, String name, T global){
        imports.add(new SuppliedImport(module, name, global));
        return this;
    }

    public Global<?> getGlobal(String module, String name) throws MissingImportException {
        return getImported(module, name, SuppliedImport::isGlobal)
                .map(p -> (Global<?>) p)
                .orElseThrow(() -> new MissingImportException(module, name, "global"));
    }

    protected Optional<Object> getImported(String module, String name, Predicate<SuppliedImport> typeCheck){
        return imports.stream()
                .filter(s -> s.module().equals(module) && s.name().equals(name) && typeCheck.test(s))
                .map(SuppliedImport::payload)
                .findFirst();
    }

}
