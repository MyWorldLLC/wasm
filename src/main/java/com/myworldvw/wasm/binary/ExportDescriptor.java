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

public record ExportDescriptor(Object payload) {

    public enum Type {
        FUNCTION_ID(FunctionId.class), TABLE_ID(TableId.class), MEMORY_ID(MemoryId.class), GLOBAL_ID(GlobalId.class);
        private final Class<?> type;

        Type(Class<?> type){
            this.type = type;
        }

        public static boolean validType(Class<?> type){
            return exportTypeOf(type).isPresent();
        }

        public static Optional<ExportDescriptor.Type> exportTypeOf(Class<?> type){
            return Arrays.stream(values()).filter(v -> v.type.equals(type)).findFirst();
        }

    }

    public ExportDescriptor {
        if(!Type.validType(payload.getClass())){
            throw new IllegalArgumentException("%s is not a valid export descriptor type".formatted(payload.getClass().getName()));
        }
    }

    public FunctionId functionId(){
        return (FunctionId) payload;
    }

    public TableId tableId(){
        return (TableId) payload;
    }

    public MemoryId memoryId(){
        return (MemoryId) payload;
    }

    public GlobalId globalId(){
        return (GlobalId) payload;
    }

    public Type type(){
        return Type.exportTypeOf(payload.getClass()).get();
    }

}
