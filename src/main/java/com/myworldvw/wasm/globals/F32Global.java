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
