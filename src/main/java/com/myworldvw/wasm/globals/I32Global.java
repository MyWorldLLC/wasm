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
