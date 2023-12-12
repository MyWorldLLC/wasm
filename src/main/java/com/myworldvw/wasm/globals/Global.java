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
