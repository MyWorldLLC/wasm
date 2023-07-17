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
