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

package com.myworldvw.wasm.binary;

import java.util.Arrays;
import java.util.Optional;

public record ImportDescriptor(Object payload) {

    public enum Type {
        TYPE_ID(TypeId.class), TABLE_TYPE(TableType.class), MEMORY_TYPE(MemoryType.class), GLOBAL_TYPE(GlobalType.class);
        private final Class<?> type;

        Type(Class<?> type){
            this.type = type;
        }

        public static boolean validType(Class<?> type){
            return importTypeOf(type).isPresent();
        }

        public static Optional<Type> importTypeOf(Class<?> type){
            return Arrays.stream(values()).filter(v -> v.type.equals(type)).findFirst();
        }

    }

    public ImportDescriptor{
        if(!Type.validType(payload.getClass())){
            throw new IllegalArgumentException("%s is not a valid import descriptor type".formatted(payload.getClass().getName()));
        }
    }

    public TypeId typeId(){
        return (TypeId) payload;
    }

    public TableType tableType(){
        return (TableType) payload;
    }

    public MemoryType memoryType(){
        return (MemoryType) payload;
    }

    public GlobalType globalType(){
        return (GlobalType) payload;
    }

    public Type type(){
        return Type.importTypeOf(payload.getClass()).get();
    }

}
