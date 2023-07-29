package com.myworldvw.wasm.globals;

public class F64Global extends Global<Double> {

    protected volatile double value;

    public static F64Global mutable(){
        return new F64Global(Mutability.VAR);
    }

    public static F64Global mutable(float value){
        return new F64Global(Mutability.VAR, value);
    }

    public static F64Global immutable(){
        return new F64Global(Mutability.CONST);
    }

    public static F64Global immutable(double value){
        return new F64Global(Mutability.CONST, value);
    }

    private F64Global(Mutability mutability, double value){
        this(mutability);
        this.value = value;
    }
    private F64Global(Mutability mutability){
        super(mutability);
    }

    @Override
    public Class<?> getType() {
        return double.class;
    }

    @Override
    public void setBoxed(Double value) {
        checkSet();
        this.value = value;
    }

    @Override
    public Double getBoxed() {
        return value;
    }

    public void setValue(double value){
        checkSet();
        this.value = value;
    }

    public double getValue(){
        return value;
    }
}
