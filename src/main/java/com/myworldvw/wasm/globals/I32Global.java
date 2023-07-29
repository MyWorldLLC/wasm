package com.myworldvw.wasm.globals;

public class I32Global extends Global<Integer> {

    protected volatile int value;

    public static I32Global mutable(){
        return new I32Global(Mutability.VAR);
    }

    public static I32Global mutable(int value){
        return new I32Global(Mutability.VAR, value);
    }

    public static I32Global immutable(){
        return new I32Global(Mutability.CONST);
    }

    public static I32Global immutable(int value){
        return new I32Global(Mutability.CONST, value);
    }

    private I32Global(Mutability mutability, int value){
        this(mutability);
        this.value = value;
    }
    private I32Global(Mutability mutability){
        super(mutability);
    }

    @Override
    public Class<?> getType() {
        return int.class;
    }

    @Override
    public void setBoxed(Integer value) {
        checkSet();
        this.value = value;
    }

    @Override
    public Integer getBoxed() {
        return value;
    }

    public void setValue(int value){
        checkSet();
        this.value = value;
    }

    public int getValue(){
        return value;
    }
}
