package com.myworldvw.wasm.globals;

public abstract class Global<T> {

    protected final Mutability mutability;

    public Global(Mutability mutability){
        this.mutability = mutability;
    }

    public abstract Class<?> getType();

    public Mutability getMutability(){
        return mutability;
    }

    public void checkSet(){
        if(mutability == Mutability.CONST){
            throw new IllegalStateException("Cannot set const global variable");
        }
    }

    public abstract void setBoxed(T value);
    public abstract T getBoxed();

}
