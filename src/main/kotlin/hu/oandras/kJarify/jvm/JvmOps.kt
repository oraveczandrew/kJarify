/*
 * Copyright (C) 2025 András Oravecz and the contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

@file:Suppress("unused", "SpellCheckingInspection")

package hu.oandras.kJarify.jvm

import androidx.collection.MutableIntObjectMap

object JvmOps {
    const val NOP = 0x00
    const val ACONST_NULL = 0x01
    const val ICONST_M1 = 0x02
    const val ICONST_0 = 0x03
    const val ICONST_1 = 0x04
    const val ICONST_2 = 0x05
    const val ICONST_3 = 0x06
    const val ICONST_4 = 0x07
    const val ICONST_5 = 0x08
    const val LCONST_0 = 0x09
    const val LCONST_1 = 0x0A
    const val FCONST_0 = 0x0B
    const val FCONST_1 = 0x0C
    const val FCONST_2 = 0x0D
    const val DCONST_0 = 0x0E
    const val DCONST_1 = 0x0F
    const val BIPUSH = 0x10
    const val SIPUSH = 0x11
    const val LDC = 0x12
    const val LDC_W = 0x13
    const val LDC2_W = 0x14
    const val ILOAD = 0x15
    const val LLOAD = 0x16
    const val FLOAD = 0x17
    const val DLOAD = 0x18
    const val ALOAD = 0x19
    const val ILOAD_0 = 0x1A
    const val ILOAD_1 = 0x1B
    const val ILOAD_2 = 0x1C
    const val ILOAD_3 = 0x1D
    const val LLOAD_0 = 0x1E
    const val LLOAD_1 = 0x1F
    const val LLOAD_2 = 0x20
    const val LLOAD_3 = 0x21
    const val FLOAD_0 = 0x22
    const val FLOAD_1 = 0x23
    const val FLOAD_2 = 0x24
    const val FLOAD_3 = 0x25
    const val DLOAD_0 = 0x26
    const val DLOAD_1 = 0x27
    const val DLOAD_2 = 0x28
    const val DLOAD_3 = 0x29
    const val ALOAD_0 = 0x2A
    const val ALOAD_1 = 0x2B
    const val ALOAD_2 = 0x2C
    const val ALOAD_3 = 0x2D
    const val IALOAD = 0x2E
    const val LALOAD = 0x2F
    const val FALOAD = 0x30
    const val DALOAD = 0x31
    const val AALOAD = 0x32
    const val BALOAD = 0x33
    const val CALOAD = 0x34
    const val SALOAD = 0x35
    const val ISTORE = 0x36
    const val LSTORE = 0x37
    const val FSTORE = 0x38
    const val DSTORE = 0x39
    const val ASTORE = 0x3A
    const val ISTORE_0 = 0x3B
    const val ISTORE_1 = 0x3C
    const val ISTORE_2 = 0x3D
    const val ISTORE_3 = 0x3E
    const val LSTORE_0 = 0x3F
    const val LSTORE_1 = 0x40
    const val LSTORE_2 = 0x41
    const val LSTORE_3 = 0x42
    const val FSTORE_0 = 0x43
    const val FSTORE_1 = 0x44
    const val FSTORE_2 = 0x45
    const val FSTORE_3 = 0x46
    const val DSTORE_0 = 0x47
    const val DSTORE_1 = 0x48
    const val DSTORE_2 = 0x49
    const val DSTORE_3 = 0x4A
    const val ASTORE_0 = 0x4B
    const val ASTORE_1 = 0x4C
    const val ASTORE_2 = 0x4D
    const val ASTORE_3 = 0x4E
    const val IASTORE = 0x4F
    const val LASTORE = 0x50
    const val FASTORE = 0x51
    const val DASTORE = 0x52
    const val AASTORE = 0x53
    const val BASTORE = 0x54
    const val CASTORE = 0x55
    const val SASTORE = 0x56
    const val POP = 0x57
    const val POP2 = 0x58
    const val DUP = 0x59
    const val DUP_X1 = 0x5A
    const val DUP_X2 = 0x5B
    const val DUP2 = 0x5C
    const val DUP2_X1 = 0x5D
    const val DUP2_X2 = 0x5E
    const val SWAP = 0x5F
    const val IADD = 0x60
    const val LADD = 0x61
    const val FADD = 0x62
    const val DADD = 0x63
    const val ISUB = 0x64
    const val LSUB = 0x65
    const val FSUB = 0x66
    const val DSUB = 0x67
    const val IMUL = 0x68
    const val LMUL = 0x69
    const val FMUL = 0x6A
    const val DMUL = 0x6B
    const val IDIV = 0x6C
    const val LDIV = 0x6D
    const val FDIV = 0x6E
    const val DDIV = 0x6F
    const val IREM = 0x70
    const val LREM = 0x71
    const val FREM = 0x72
    const val DREM = 0x73
    const val INEG = 0x74
    const val LNEG = 0x75
    const val FNEG = 0x76
    const val DNEG = 0x77
    const val ISHL = 0x78
    const val LSHL = 0x79
    const val ISHR = 0x7A
    const val LSHR = 0x7B
    const val IUSHR = 0x7C
    const val LUSHR = 0x7D
    const val IAND = 0x7E
    const val LAND = 0x7F
    const val IOR = 0x80
    const val LOR = 0x81
    const val IXOR = 0x82
    const val LXOR = 0x83
    const val IINC = 0x84
    const val I2L = 0x85
    const val I2F = 0x86
    const val I2D = 0x87
    const val L2I = 0x88
    const val L2F = 0x89
    const val L2D = 0x8A
    const val F2I = 0x8B
    const val F2L = 0x8C
    const val F2D = 0x8D
    const val D2I = 0x8E
    const val D2L = 0x8F
    const val D2F = 0x90
    const val I2B = 0x91
    const val I2C = 0x92
    const val I2S = 0x93
    const val LCMP = 0x94
    const val FCMPL = 0x95
    const val FCMPG = 0x96
    const val DCMPL = 0x97
    const val DCMPG = 0x98
    const val IFEQ = 0x99
    const val IFNE = 0x9A
    const val IFLT = 0x9B
    const val IFGE = 0x9C
    const val IFGT = 0x9D
    const val IFLE = 0x9E
    const val IF_ICMPEQ = 0x9F
    const val IF_ICMPNE = 0xA0
    const val IF_ICMPLT = 0xA1
    const val IF_ICMPGE = 0xA2
    const val IF_ICMPGT = 0xA3
    const val IF_ICMPLE = 0xA4
    const val IF_ACMPEQ = 0xA5
    const val IF_ACMPNE = 0xA6
    const val GOTO = 0xA7
    const val JSR = 0xA8
    const val RET = 0xA9
    const val TABLESWITCH = 0xAA
    const val LOOKUPSWITCH = 0xAB
    const val IRETURN = 0xAC
    const val LRETURN = 0xAD
    const val FRETURN = 0xAE
    const val DRETURN = 0xAF
    const val ARETURN = 0xB0
    const val RETURN = 0xB1
    const val GETSTATIC = 0xB2
    const val PUTSTATIC = 0xB3
    const val GETFIELD = 0xB4
    const val PUTFIELD = 0xB5
    const val INVOKEVIRTUAL = 0xB6
    const val INVOKESPECIAL = 0xB7
    const val INVOKESTATIC = 0xB8
    const val INVOKEINTERFACE = 0xB9
    const val INVOKEDYNAMIC = 0xBA
    const val NEW = 0xBB
    const val NEWARRAY = 0xBC
    const val ANEWARRAY = 0xBD
    const val ARRAYLENGTH = 0xBE
    const val ATHROW = 0xBF
    const val CHECKCAST = 0xC0
    const val INSTANCEOF = 0xC1
    const val MONITORENTER = 0xC2
    const val MONITOREXIT = 0xC3
    const val WIDE = 0xC4
    const val MULTIANEWARRAY = 0xC5
    const val IFNULL = 0xC6
    const val IFNONNULL = 0xC7
    const val GOTO_W = 0xC8
    const val JSR_W = 0xC9

    private val opToStr = MutableIntObjectMap<String>().apply {
        put(0x00, "NOP")
        put(0x01, "ACONST_NULL")
        put(0x02, "ICONST_M1")
        put(0x03, "ICONST_0")
        put(0x04, "ICONST_1")
        put(0x05, "ICONST_2")
        put(0x06, "ICONST_3")
        put(0x07, "ICONST_4")
        put(0x08, "ICONST_5")
        put(0x09, "LCONST_0")
        put(0x0A, "LCONST_1")
        put(0x0B, "FCONST_0")
        put(0x0C, "FCONST_1")
        put(0x0D, "FCONST_2")
        put(0x0E, "DCONST_0")
        put(0x0F, "DCONST_1")
        put(0x10, "BIPUSH")
        put(0x11, "SIPUSH")
        put(0x12, "LDC")
        put(0x13, "LDC_W")
        put(0x14, "LDC2_W")
        put(0x15, "ILOAD")
        put(0x16, "LLOAD")
        put(0x17, "FLOAD")
        put(0x18, "DLOAD")
        put(0x19, "ALOAD")
        put(0x1A, "ILOAD_0")
        put(0x1B, "ILOAD_1")
        put(0x1C, "ILOAD_2")
        put(0x1D, "ILOAD_3")
        put(0x1E, "LLOAD_0")
        put(0x1F, "LLOAD_1")
        put(0x20, "LLOAD_2")
        put(0x21, "LLOAD_3")
        put(0x22, "FLOAD_0")
        put(0x23, "FLOAD_1")
        put(0x24, "FLOAD_2")
        put(0x25, "FLOAD_3")
        put(0x26, "DLOAD_0")
        put(0x27, "DLOAD_1")
        put(0x28, "DLOAD_2")
        put(0x29, "DLOAD_3")
        put(0x2A, "ALOAD_0")
        put(0x2B, "ALOAD_1")
        put(0x2C, "ALOAD_2")
        put(0x2D, "ALOAD_3")
        put(0x2E, "IALOAD")
        put(0x2F, "LALOAD")
        put(0x30, "FALOAD")
        put(0x31, "DALOAD")
        put(0x32, "AALOAD")
        put(0x33, "BALOAD")
        put(0x34, "CALOAD")
        put(0x35, "SALOAD")
        put(0x36, "ISTORE")
        put(0x37, "LSTORE")
        put(0x38, "FSTORE")
        put(0x39, "DSTORE")
        put(0x3A, "ASTORE")
        put(0x3B, "ISTORE_0")
        put(0x3C, "ISTORE_1")
        put(0x3D, "ISTORE_2")
        put(0x3E, "ISTORE_3")
        put(0x3F, "LSTORE_0")
        put(0x40, "LSTORE_1")
        put(0x41, "LSTORE_2")
        put(0x42, "LSTORE_3")
        put(0x43, "FSTORE_0")
        put(0x44, "FSTORE_1")
        put(0x45, "FSTORE_2")
        put(0x46, "FSTORE_3")
        put(0x47, "DSTORE_0")
        put(0x48, "DSTORE_1")
        put(0x49, "DSTORE_2")
        put(0x4A, "DSTORE_3")
        put(0x4B, "ASTORE_0")
        put(0x4C, "ASTORE_1")
        put(0x4D, "ASTORE_2")
        put(0x4E, "ASTORE_3")
        put(0x4F, "IASTORE")
        put(0x50, "LASTORE")
        put(0x51, "FASTORE")
        put(0x52, "DASTORE")
        put(0x53, "AASTORE")
        put(0x54, "BASTORE")
        put(0x55, "CASTORE")
        put(0x56, "SASTORE")
        put(0x57, "POP")
        put(0x58, "POP2")
        put(0x59, "DUP")
        put(0x5A, "DUP_X1")
        put(0x5B, "DUP_X2")
        put(0x5C, "DUP2")
        put(0x5D, "DUP2_X1")
        put(0x5E, "DUP2_X2")
        put(0x5F, "SWAP")
        put(0x60, "IADD")
        put(0x61, "LADD")
        put(0x62, "FADD")
        put(0x63, "DADD")
        put(0x64, "ISUB")
        put(0x65, "LSUB")
        put(0x66, "FSUB")
        put(0x67, "DSUB")
        put(0x68, "IMUL")
        put(0x69, "LMUL")
        put(0x6A, "FMUL")
        put(0x6B, "DMUL")
        put(0x6C, "IDIV")
        put(0x6D, "LDIV")
        put(0x6E, "FDIV")
        put(0x6F, "DDIV")
        put(0x70, "IREM")
        put(0x71, "LREM")
        put(0x72, "FREM")
        put(0x73, "DREM")
        put(0x74, "INEG")
        put(0x75, "LNEG")
        put(0x76, "FNEG")
        put(0x77, "DNEG")
        put(0x78, "ISHL")
        put(0x79, "LSHL")
        put(0x7A, "ISHR")
        put(0x7B, "LSHR")
        put(0x7C, "IUSHR")
        put(0x7D, "LUSHR")
        put(0x7E, "IAND")
        put(0x7F, "LAND")
        put(0x80, "IOR")
        put(0x81, "LOR")
        put(0x82, "IXOR")
        put(0x83, "LXOR")
        put(0x84, "IINC")
        put(0x85, "I2L")
        put(0x86, "I2F")
        put(0x87, "I2D")
        put(0x88, "L2I")
        put(0x89, "L2F")
        put(0x8A, "L2D")
        put(0x8B, "F2I")
        put(0x8C, "F2L")
        put(0x8D, "F2D")
        put(0x8E, "D2I")
        put(0x8F, "D2L")
        put(0x90, "D2F")
        put(0x91, "I2B")
        put(0x92, "I2C")
        put(0x93, "I2S")
        put(0x94, "LCMP")
        put(0x95, "FCMPL")
        put(0x96, "FCMPG")
        put(0x97, "DCMPL")
        put(0x98, "DCMPG")
        put(0x99, "IFEQ")
        put(0x9A, "IFNE")
        put(0x9B, "IFLT")
        put(0x9C, "IFGE")
        put(0x9D, "IFGT")
        put(0x9E, "IFLE")
        put(0x9F, "IF_ICMPEQ")
        put(0xA0, "IF_ICMPNE")
        put(0xA1, "IF_ICMPLT")
        put(0xA2, "IF_ICMPGE")
        put(0xA3, "IF_ICMPGT")
        put(0xA4, "IF_ICMPLE")
        put(0xA5, "IF_ACMPEQ")
        put(0xA6, "IF_ACMPNE")
        put(0xA7, "GOTO")
        put(0xA8, "JSR")
        put(0xA9, "RET")
        put(0xAA, "TABLESWITCH")
        put(0xAB, "LOOKUPSWITCH")
        put(0xAC, "IRETURN")
        put(0xAD, "LRETURN")
        put(0xAE, "FRETURN")
        put(0xAF, "DRETURN")
        put(0xB0, "ARETURN")
        put(0xB1, "RETURN")
        put(0xB2, "GETSTATIC")
        put(0xB3, "PUTSTATIC")
        put(0xB4, "GETFIELD")
        put(0xB5, "PUTFIELD")
        put(0xB6, "INVOKEVIRTUAL")
        put(0xB7, "INVOKESPECIAL")
        put(0xB8, "INVOKESTATIC")
        put(0xB9, "INVOKEINTERFACE")
        put(0xBA, "INVOKEDYNAMIC")
        put(0xBB, "NEW")
        put(0xBC, "NEWARRAY")
        put(0xBD, "ANEWARRAY")
        put(0xBE, "ARRAYLENGTH")
        put(0xBF, "ATHROW")
        put(0xC0, "CHECKCAST")
        put(0xC1, "INSTANCEOF")
        put(0xC2, "MONITORENTER")
        put(0xC3, "MONITOREXIT")
        put(0xC4, "WIDE")
        put(0xC5, "MULTIANEWARRAY")
        put(0xC6, "IFNULL")
        put(0xC7, "IFNONNULL")
        put(0xC8, "GOTO_W")
        put(0xC9, "JSR_W")
    }

    fun opToStr(op: Byte): String {
        return opToStr.getOrDefault(op.toUByte().toInt(), "_Unknown opcode_")
    }
}