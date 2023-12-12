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

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Optional;

public class Table {

    protected MethodHandle[] entries;
    protected final Optional<Integer> maxSize;

    public Table(){
        this(Optional.empty());
    }

    public Table(Optional<Integer> maxSize){
        this.maxSize = maxSize;
    }

    public Optional<Integer> getMaxSize(){
        return maxSize;
    }

    public void set(int id, MethodHandle handle){
        ensureSize(id);
        entries[id] = handle;
    }

    public void setAll(int startId, MethodHandle[] handles){
        // Bulk assign method handles, starting from the highest index
        // (so that we resize the backing array at most once)
        for(int i = handles.length - 1; i >= 0; i--){
            set(startId + i, handles[i]);
        }
    }

    public MethodHandle get(int id){
        ensureSize(id);
        return entries[id];
    }

    protected void ensureSize(int id){

        if(entries != null && id < entries.length){
            return;
        }

        if(maxSize.map(m -> id < m).orElse(true)){
            if(entries == null){
                entries = new MethodHandle[Math.max(1, id)];
            }else{
                entries = Arrays.copyOf(entries, id);
            }
        }else{
            throw new IllegalArgumentException("Cannot resize to accomodate %d: max size is %d".formatted(id, maxSize.get()));
        }
    }

}
