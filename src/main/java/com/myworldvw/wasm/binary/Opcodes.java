package com.myworldvw.wasm.binary;

public class Opcodes {

    // ==== Structure/Control ====
    public static final byte UNREACHABLE = 0x00;
    public static final byte NOP         = 0x01;
    public static final byte END           = 0x0B;
    public static final byte BLOCK         = 0x02;
    public static final byte LOOP          = 0x03;
    public static final byte IF            = 0x04;
    public static final byte ELSE          = 0x05;
    public static final byte BR            = 0x0C;
    public static final byte BR_IF         = 0x0D;
    public static final byte BR_TABLE      = 0x0E;
    public static final byte RETURN        = 0x0F;
    public static final byte CALL          = 0x10;
    public static final byte CALL_INDIRECT = 0x11;

    // ==== Parametric ====
    public static final byte DROP          = 0x1A;
    public static final byte SELECT        = 0x1B;

    // ==== Variable ====
    public static final byte LOCAL_GET     = 0x20;
    public static final byte LOCAL_SET     = 0x21;
    public static final byte LOCAL_TEE     = 0x22;
    public static final byte GLOBAL_GET    = 0x23;
    public static final byte GLOBAL_SET    = 0x24;

    // ==== Memory ====
    public static final byte I32_LOAD      = 0x28;
    public static final byte I64_LOAD      = 0x29;
    public static final byte F32_LOAD      = 0x2A;
    public static final byte F64_LOAD      = 0x2B;
    public static final byte I32_LOAD_8_S  = 0x2C;
    public static final byte I32_LOAD_8_U  = 0x2D;
    public static final byte I32_LOAD_16_S = 0x2E;
    public static final byte I32_LOAD_16_U = 0x2F;
    public static final byte I64_LOAD_8_S  = 0x30;
    public static final byte I64_LOAD_8_U  = 0x31;
    public static final byte I64_LOAD_16_S = 0x32;
    public static final byte I64_LOAD_16_U = 0x33;
    public static final byte I64_LOAD_32_S = 0x34;
    public static final byte I64_LOAD_32_U = 0x35;
    public static final byte I32_STORE     = 0x36;
    public static final byte I64_STORE     = 0x37;
    public static final byte F32_STORE     = 0x38;
    public static final byte F64_STORE     = 0x39;
    public static final byte I32_STORE_8   = 0x3A;
    public static final byte I32_STORE_16  = 0x3B;
    public static final byte I64_STORE_8   = 0x3C;
    public static final byte I64_STORE_16  = 0x3D;
    public static final byte I64_STORE_32  = 0x3E;
    public static final byte MEMORY_SIZE   = 0x3F;
    public static final byte MEMORY_GROW   = 0x40;

    // ==== Numeric ====
    public static final byte I32_CONST     = 0x41;
    public static final byte I64_CONST     = 0x42;
    public static final byte F32_CONST     = 0x43;
    public static final byte F64_CONST     = 0x44;
    // TODO
}
