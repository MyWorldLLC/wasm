package com.myworldvw.wasm.util;

import com.myworldvw.wasm.WasmContext;
import com.myworldvw.wasm.binary.WasmFormatException;

import java.io.IOException;

public class WasmLoader {

    public static WasmContext createFromResources(String... resourcePaths) throws WasmFormatException, IOException {
        var ctx = new WasmContext();

        for(var path : resourcePaths){
            try(var in = WasmContext.class.getResourceAsStream(path)){
                var segments = path.split("/");
                var namePiece = segments[segments.length - 1];

                var name = namePiece.indexOf('.') != -1 ? namePiece.substring(0, namePiece.indexOf('.')) : namePiece;
                ctx.loadBinary(name, in);
            }
        }

        ctx.compileAll();

        return ctx;
    }

}
