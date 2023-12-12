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
