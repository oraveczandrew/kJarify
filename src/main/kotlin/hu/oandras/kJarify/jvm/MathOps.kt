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

package hu.oandras.kJarify.jvm

import androidx.collection.IntIntMap
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntObjectMap


internal object MathOps {

    @JvmField
    val UNARY: IntObjectMap<IntArray> = MutableIntObjectMap<IntArray>().apply {
        put(0x7B, intArrayOf(JvmOps.INEG, Scalars.INT, Scalars.INT))
        put(0x7C, intArrayOf(JvmOps.IXOR, Scalars.INT, Scalars.INT))
        put(0x7D, intArrayOf(JvmOps.LNEG, Scalars.LONG, Scalars.LONG))
        put(0x7E, intArrayOf(JvmOps.LXOR, Scalars.LONG, Scalars.LONG))
        put(0x7F, intArrayOf(JvmOps.FNEG, Scalars.FLOAT, Scalars.FLOAT))
        put(0x80, intArrayOf(JvmOps.DNEG, Scalars.DOUBLE, Scalars.DOUBLE))
        put(0x81, intArrayOf(JvmOps.I2L, Scalars.INT, Scalars.LONG))
        put(0x82, intArrayOf(JvmOps.I2F, Scalars.INT, Scalars.FLOAT))
        put(0x83, intArrayOf(JvmOps.I2D, Scalars.INT, Scalars.DOUBLE))
        put(0x84, intArrayOf(JvmOps.L2I, Scalars.LONG, Scalars.INT))
        put(0x85, intArrayOf(JvmOps.L2F, Scalars.LONG, Scalars.FLOAT))
        put(0x86, intArrayOf(JvmOps.L2D, Scalars.LONG, Scalars.DOUBLE))
        put(0x87, intArrayOf(JvmOps.F2I, Scalars.FLOAT, Scalars.INT))
        put(0x88, intArrayOf(JvmOps.F2L, Scalars.FLOAT, Scalars.LONG))
        put(0x89, intArrayOf(JvmOps.F2D, Scalars.FLOAT, Scalars.DOUBLE))
        put(0x8A, intArrayOf(JvmOps.D2I, Scalars.DOUBLE, Scalars.INT))
        put(0x8B, intArrayOf(JvmOps.D2L, Scalars.DOUBLE, Scalars.LONG))
        put(0x8C, intArrayOf(JvmOps.D2F, Scalars.DOUBLE, Scalars.FLOAT))
        put(0x8D, intArrayOf(JvmOps.I2B, Scalars.INT, Scalars.INT))
        put(0x8E, intArrayOf(JvmOps.I2C, Scalars.INT, Scalars.INT))
        put(0x8F, intArrayOf(JvmOps.I2S, Scalars.INT, Scalars.INT))
    }

    @JvmField
    val BINARY: IntObjectMap<IntArray> = MutableIntObjectMap<IntArray>().apply {
        put(0x90, intArrayOf(JvmOps.IADD, Scalars.INT, Scalars.INT))
        put(0x91, intArrayOf(JvmOps.ISUB, Scalars.INT, Scalars.INT))
        put(0x92, intArrayOf(JvmOps.IMUL, Scalars.INT, Scalars.INT))
        put(0x93, intArrayOf(JvmOps.IDIV, Scalars.INT, Scalars.INT))
        put(0x94, intArrayOf(JvmOps.IREM, Scalars.INT, Scalars.INT))
        put(0x95, intArrayOf(JvmOps.IAND, Scalars.INT, Scalars.INT))
        put(0x96, intArrayOf(JvmOps.IOR, Scalars.INT, Scalars.INT))
        put(0x97, intArrayOf(JvmOps.IXOR, Scalars.INT, Scalars.INT))
        put(0x98, intArrayOf(JvmOps.ISHL, Scalars.INT, Scalars.INT))
        put(0x99, intArrayOf(JvmOps.ISHR, Scalars.INT, Scalars.INT))
        put(0x9A, intArrayOf(JvmOps.IUSHR, Scalars.INT, Scalars.INT))
        put(0x9B, intArrayOf(JvmOps.LADD, Scalars.LONG, Scalars.LONG))
        put(0x9C, intArrayOf(JvmOps.LSUB, Scalars.LONG, Scalars.LONG))
        put(0x9D, intArrayOf(JvmOps.LMUL, Scalars.LONG, Scalars.LONG))
        put(0x9E, intArrayOf(JvmOps.LDIV, Scalars.LONG, Scalars.LONG))
        put(0x9F, intArrayOf(JvmOps.LREM, Scalars.LONG, Scalars.LONG))
        put(0xA0, intArrayOf(JvmOps.LAND, Scalars.LONG, Scalars.LONG))
        put(0xA1, intArrayOf(JvmOps.LOR, Scalars.LONG, Scalars.LONG))
        put(0xA2, intArrayOf(JvmOps.LXOR, Scalars.LONG, Scalars.LONG))
        put(0xA3, intArrayOf(JvmOps.LSHL, Scalars.LONG, Scalars.INT))
        put(0xA4, intArrayOf(JvmOps.LSHR, Scalars.LONG, Scalars.INT))
        put(0xA5, intArrayOf(JvmOps.LUSHR, Scalars.LONG, Scalars.INT))
        put(0xA6, intArrayOf(JvmOps.FADD, Scalars.FLOAT, Scalars.FLOAT))
        put(0xA7, intArrayOf(JvmOps.FSUB, Scalars.FLOAT, Scalars.FLOAT))
        put(0xA8, intArrayOf(JvmOps.FMUL, Scalars.FLOAT, Scalars.FLOAT))
        put(0xA9, intArrayOf(JvmOps.FDIV, Scalars.FLOAT, Scalars.FLOAT))
        put(0xAA, intArrayOf(JvmOps.FREM, Scalars.FLOAT, Scalars.FLOAT))
        put(0xAB, intArrayOf(JvmOps.DADD, Scalars.DOUBLE, Scalars.DOUBLE))
        put(0xAC, intArrayOf(JvmOps.DSUB, Scalars.DOUBLE, Scalars.DOUBLE))
        put(0xAD, intArrayOf(JvmOps.DMUL, Scalars.DOUBLE, Scalars.DOUBLE))
        put(0xAE, intArrayOf(JvmOps.DDIV, Scalars.DOUBLE, Scalars.DOUBLE))
        put(0xAF, intArrayOf(JvmOps.DREM, Scalars.DOUBLE, Scalars.DOUBLE))
        put(0xB0, intArrayOf(JvmOps.IADD, Scalars.INT, Scalars.INT))
        put(0xB1, intArrayOf(JvmOps.ISUB, Scalars.INT, Scalars.INT))
        put(0xB2, intArrayOf(JvmOps.IMUL, Scalars.INT, Scalars.INT))
        put(0xB3, intArrayOf(JvmOps.IDIV, Scalars.INT, Scalars.INT))
        put(0xB4, intArrayOf(JvmOps.IREM, Scalars.INT, Scalars.INT))
        put(0xB5, intArrayOf(JvmOps.IAND, Scalars.INT, Scalars.INT))
        put(0xB6, intArrayOf(JvmOps.IOR, Scalars.INT, Scalars.INT))
        put(0xB7, intArrayOf(JvmOps.IXOR, Scalars.INT, Scalars.INT))
        put(0xB8, intArrayOf(JvmOps.ISHL, Scalars.INT, Scalars.INT))
        put(0xB9, intArrayOf(JvmOps.ISHR, Scalars.INT, Scalars.INT))
        put(0xBA, intArrayOf(JvmOps.IUSHR, Scalars.INT, Scalars.INT))
        put(0xBB, intArrayOf(JvmOps.LADD, Scalars.LONG, Scalars.LONG))
        put(0xBC, intArrayOf(JvmOps.LSUB, Scalars.LONG, Scalars.LONG))
        put(0xBD, intArrayOf(JvmOps.LMUL, Scalars.LONG, Scalars.LONG))
        put(0xBE, intArrayOf(JvmOps.LDIV, Scalars.LONG, Scalars.LONG))
        put(0xBF, intArrayOf(JvmOps.LREM, Scalars.LONG, Scalars.LONG))
        put(0xC0, intArrayOf(JvmOps.LAND, Scalars.LONG, Scalars.LONG))
        put(0xC1, intArrayOf(JvmOps.LOR, Scalars.LONG, Scalars.LONG))
        put(0xC2, intArrayOf(JvmOps.LXOR, Scalars.LONG, Scalars.LONG))
        put(0xC3, intArrayOf(JvmOps.LSHL, Scalars.LONG, Scalars.INT))
        put(0xC4, intArrayOf(JvmOps.LSHR, Scalars.LONG, Scalars.INT))
        put(0xC5, intArrayOf(JvmOps.LUSHR, Scalars.LONG, Scalars.INT))
        put(0xC6, intArrayOf(JvmOps.FADD, Scalars.FLOAT, Scalars.FLOAT))
        put(0xC7, intArrayOf(JvmOps.FSUB, Scalars.FLOAT, Scalars.FLOAT))
        put(0xC8, intArrayOf(JvmOps.FMUL, Scalars.FLOAT, Scalars.FLOAT))
        put(0xC9, intArrayOf(JvmOps.FDIV, Scalars.FLOAT, Scalars.FLOAT))
        put(0xCA, intArrayOf(JvmOps.FREM, Scalars.FLOAT, Scalars.FLOAT))
        put(0xCB, intArrayOf(JvmOps.DADD, Scalars.DOUBLE, Scalars.DOUBLE))
        put(0xCC, intArrayOf(JvmOps.DSUB, Scalars.DOUBLE, Scalars.DOUBLE))
        put(0xCD, intArrayOf(JvmOps.DMUL, Scalars.DOUBLE, Scalars.DOUBLE))
        put(0xCE, intArrayOf(JvmOps.DDIV, Scalars.DOUBLE, Scalars.DOUBLE))
        put(0xCF, intArrayOf(JvmOps.DREM, Scalars.DOUBLE, Scalars.DOUBLE))
    }

    @JvmField
    val BINARY_LIT: IntIntMap = MutableIntIntMap().apply {
        put(0xD0, JvmOps.IADD)
        put(0xD1, JvmOps.ISUB)
        put(0xD2, JvmOps.IMUL)
        put(0xD3, JvmOps.IDIV)
        put(0xD4, JvmOps.IREM)
        put(0xD5, JvmOps.IAND)
        put(0xD6, JvmOps.IOR)
        put(0xD7, JvmOps.IXOR)
        put(0xD8, JvmOps.IADD)
        put(0xD9, JvmOps.ISUB)
        put(0xDA, JvmOps.IMUL)
        put(0xDB, JvmOps.IDIV)
        put(0xDC, JvmOps.IREM)
        put(0xDD, JvmOps.IAND)
        put(0xDE, JvmOps.IOR)
        put(0xDF, JvmOps.IXOR)
        put(0xE0, JvmOps.ISHL)
        put(0xE1, JvmOps.ISHR)
        put(0xE2, JvmOps.IUSHR)
    }
}
