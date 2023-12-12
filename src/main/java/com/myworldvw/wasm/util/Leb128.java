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

package com.myworldvw.wasm.util;

import java.nio.ByteBuffer;

public class Leb128 {

    public static long decodeUnsigned(ByteBuffer in){
        long result = 0;
        int shift = 0;

        while(true){
            byte b = in.get();
            result |= (long) (b & 0b0111_111) << shift;

            if((b & 0x1000_0000) == 0){
                return result;
            }

            shift += 7;
        }
    }

    public static long decodeSigned(ByteBuffer in, int bitLength){
        long result = 0;
        long shift = 0;

        byte b;
        do {
            b = in.get();
            result |= (long) (b & 0b0111_111) << shift;
            shift += 7;
        } while((b & 0x1000_0000) != 0);

        if ((shift < bitLength) && (b & 0x0100_0000) != 0){
            // extend sign
            result |= (~0L << shift);
        }

        return result;
    }
}
