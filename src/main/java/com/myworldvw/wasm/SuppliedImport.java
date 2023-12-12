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

package com.myworldvw.wasm;

import com.myworldvw.wasm.globals.Global;

import java.lang.invoke.MethodHandle;

public record SuppliedImport(String module, String name, Object payload) {

    public boolean isMemory(){
        return payload instanceof Memory;
    }

    public boolean isTable(){
        return payload instanceof Table;
    }

    public boolean isFunction(){
        return payload instanceof MethodHandle;
    }

    public boolean isGlobal(){
        return payload instanceof Global;
    }

}
