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
