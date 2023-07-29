package com.myworldvw.wasm.globals;

public class I64Global extends Global<Long> {

    protected volatile long value;

    public static I64Global mutable(){
        return new I64Global(Mutability.VAR);
    }

    public static I64Global mutable(long value){
        return new I64Global(Mutability.VAR, value);
    }

    public static I64Global immutable(){
        return new I64Global(Mutability.CONST);
    }

    public static I64Global immutable(long value){
        return new I64Global(Mutability.CONST, value);
    }

    private I64Global(Mutability mutability, long value){
        this(mutability);
        this.value = value;
    }
    private I64Global(Mutability mutability){
        super(mutability);
    }

    @Override
    public Class<?> getType() {
        return long.class;
    }

    @Override
    public void setBoxed(Long value) {
        checkSet();
        this.value = value;
    }

    @Override
    public Long getBoxed() {
        return value;
    }

    public void setValue(long value){
        checkSet();
        this.value = value;
    }

    public long getValue(){
        return value;
    }
}
