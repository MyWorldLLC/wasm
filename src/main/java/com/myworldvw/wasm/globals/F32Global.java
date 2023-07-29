package com.myworldvw.wasm.globals;

public class F32Global extends Global<Float> {

    protected volatile float value;

    public static F32Global mutable(){
        return new F32Global(Mutability.VAR);
    }

    public static F32Global mutable(float value){
        return new F32Global(Mutability.VAR, value);
    }

    public static F32Global immutable(){
        return new F32Global(Mutability.CONST);
    }

    public static F32Global immutable(float value){
        return new F32Global(Mutability.CONST, value);
    }

    private F32Global(Mutability mutability, float value){
        this(mutability);
        this.value = value;
    }
    private F32Global(Mutability mutability){
        super(mutability);
    }

    @Override
    public Class<?> getType() {
        return float.class;
    }

    @Override
    public void setBoxed(Float value) {
        checkSet();
        this.value = value;
    }

    @Override
    public Float getBoxed() {
        return value;
    }

    public void setValue(float value){
        checkSet();
        this.value = value;
    }

    public float getValue(){
        return value;
    }

}
