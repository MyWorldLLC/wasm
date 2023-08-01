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
