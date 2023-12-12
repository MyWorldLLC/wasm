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

public class WasmConfig {

    protected int trapFlags;

    protected String modulePackage;

    public WasmConfig(){
    }

    public String getCompiledModulePackage() {
        return modulePackage;
    }

    public void setCompiledModulePackage(String modulePackage) {
        this.modulePackage = modulePackage;
    }

    public String getCompiledClassName(String name){
        if(modulePackage != null){
            return modulePackage + "." + name;
        }
        return name;
    }
}