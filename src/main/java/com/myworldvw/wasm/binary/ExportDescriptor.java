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
