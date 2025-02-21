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

package hu.oandras.kJarify.jvm.constants

import androidx.collection.*
import hu.oandras.kJarify.byteArrayOf_u8
import hu.oandras.kJarify.byteArrayOf_u8u16
import hu.oandras.kJarify.byteArrayOf_u8u8
import hu.oandras.kJarify.byteArrayOf_u8u8u8
import hu.oandras.kJarify.byteArrayOf_u8u8u8u8
import hu.oandras.kJarify.concatenate
import hu.oandras.kJarify.jvm.JvmOps.BIPUSH
import hu.oandras.kJarify.jvm.JvmOps.DCONST_0
import hu.oandras.kJarify.jvm.JvmOps.DCONST_1
import hu.oandras.kJarify.jvm.JvmOps.DDIV
import hu.oandras.kJarify.jvm.JvmOps.DNEG
import hu.oandras.kJarify.jvm.JvmOps.FCONST_0
import hu.oandras.kJarify.jvm.JvmOps.FCONST_1
import hu.oandras.kJarify.jvm.JvmOps.FDIV
import hu.oandras.kJarify.jvm.JvmOps.FNEG
import hu.oandras.kJarify.jvm.JvmOps.I2C
import hu.oandras.kJarify.jvm.JvmOps.I2D
import hu.oandras.kJarify.jvm.JvmOps.I2F
import hu.oandras.kJarify.jvm.JvmOps.I2L
import hu.oandras.kJarify.jvm.JvmOps.ICONST_0
import hu.oandras.kJarify.jvm.JvmOps.INEG
import hu.oandras.kJarify.jvm.JvmOps.ISHL
import hu.oandras.kJarify.jvm.JvmOps.ISHR
import hu.oandras.kJarify.jvm.JvmOps.IUSHR
import hu.oandras.kJarify.jvm.JvmOps.LCONST_0
import hu.oandras.kJarify.jvm.JvmOps.SIPUSH

typealias LongLookupTable = LongObjectMap<ByteArray>
typealias IntLookupTable = IntObjectMap<ByteArray>

@Suppress("LocalVariableName")
internal object GenLookUp {
    const val FLOAT_SIGN = 1 shl 31
    const val FLOAT_NAN = -1
    const val FLOAT_INF = 0xFF shl 23
    const val FLOAT_NINF = FLOAT_INF xor FLOAT_SIGN

    @JvmStatic
    private fun i2f(x: Int): Int {
        if (x == 0) {
            return 0
        }
        if (x < 0) {
            return i2f(-x) xor FLOAT_SIGN
        }
        val shift = 24 - (Integer.SIZE - x.countLeadingZeroBits())
        // Don't bother implementing rounding since we'll only convert small ints
        // that can be exactly represented anyway
        assert(shift >= 0)
        val mantissa = x shl shift
        val exponent = shift + 127
        return (exponent shl 23) or mantissa
    }

    const val DOUBLE_SIGN: Long = 1L shl 63
    const val DOUBLE_NAN: Long = -1L
    const val DOUBLE_INF: Long = 0x7FFL shl 52
    const val DOUBLE_NINF: Long = DOUBLE_INF xor DOUBLE_SIGN

    @JvmStatic
    private fun i2d(x: Long): Long {
        if (x == 0L) {
            return 0
        }
        if (x < 0) {
            return i2d(-x) xor DOUBLE_SIGN
        }
        val shift = 53 - (java.lang.Long.SIZE - x.countLeadingZeroBits())
        assert(shift >= 0)
        val mantissa = x shl shift
        val exponent = shift + 1023
        return exponent.toLong() shl 52 or mantissa
    }

    // add if value is shorter than current best
    @JvmStatic
    private fun add(d: MutableLongObjectMap<ByteArray>, k: Long, v: ByteArray) {
        if (!d.containsKey(k) || v.size < d[k]!!.size) {
            d.put(k, v)
        }
    }

    @JvmStatic
    private fun add(d: MutableIntObjectMap<ByteArray>, k: Int, v: ByteArray) {
        if (!d.containsKey(k) || v.size < d[k]!!.size) {
            d.put(k, v)
        }
    }

    @JvmStatic
    fun generate(): LookupTables {
        val i2cBytes = byteArrayOf_u8(I2C)
        val i2lBytes = byteArrayOf_u8(I2L)
        val i2fBytes = byteArrayOf_u8(I2F)
        val i2dBytes = byteArrayOf_u8(I2D)
        val iNegBytes = byteArrayOf_u8(INEG)
        val ISHLBytes = byteArrayOf_u8(ISHL)
        val ISHRBytes = byteArrayOf_u8(ISHR)
        val IUSHRBytes = byteArrayOf_u8(IUSHR)

        // int constants
        val allInts = MutableIntObjectMap<ByteArray>(65537)

        // 1 byte ints
        for (i in -1..5) {
            add(allInts, i, byteArrayOf_u8(ICONST_0 + i))
        }
        
        // Sort for determinism
        val int1s = MutableIntList()
        allInts.forEach { key, value ->
            if (value.size == 1) {
                int1s.add(key)
            }
        }
        int1s.sort()

        // 2 byte ints
        for (i in -128..127) {
            add(allInts, i, byteArrayOf_u8u8(BIPUSH, i))
        }
        for (k in int1s.indices) {
            val i = int1s[k]
            add(allInts, i % 65536, concatenate(allInts[i]!!, i2cBytes))
        }

        val int2s = MutableIntList()
        allInts.forEach { key, value ->
            if (value.size == 2) {
                int2s.add(key)
            }
        }
        int2s.sort()

        // 3 byte ints
        for (i in -32768..32767) {
            add(
                d = allInts,
                k = i,
                v = byteArrayOf_u8u16(
                    SIPUSH,
                    i
                )
            )
        }

        for (k in int2s.indices) {
            val i = int2s[k]
            val bytes = allInts[i]!!
            add(allInts, i % 65536, concatenate(bytes, i2cBytes))
            add(allInts, -i, concatenate(bytes, iNegBytes))
        }

        for (i in int1s.indices) {
            val x = int1s[i]

            for (k in int1s.indices) {
                val y = int1s[k]

                val allIntsXBytes = allInts[x]!!
                val allIntsYBytes = allInts[y]!!

                add(
                    allInts,
                    x shl y % 32,
                    concatenate(
                        allIntsXBytes,
                        allIntsYBytes,
                        ISHLBytes
                    )
                )
                add(
                    allInts,
                    x shr y % 32,
                    concatenate(
                        allIntsXBytes,
                        allIntsYBytes,
                        ISHRBytes
                    )
                )
                add(
                    allInts,
                    x ushr y % 32,
                    concatenate(
                        allIntsXBytes,
                        allIntsYBytes,
                        IUSHRBytes
                    )
                )
            }
        }

        val int1AndInt2s = MutableIntList()
        int1AndInt2s += int1s
        int1AndInt2s += int2s

        // long constants
        val allLongs = MutableLongObjectMap<ByteArray>(256)
        for (i in 0..1) {
            add(allLongs, i.toLong(), byteArrayOf_u8(LCONST_0 + i))
        }

        int1AndInt2s.forEach { i ->
            add(allLongs, i.toLong(), concatenate(allInts[i]!!, i2lBytes))
        }

        // float constants
        val allFloats: MutableIntObjectMap<ByteArray> = MutableIntObjectMap(272)
        for (i in 0..1) {
            add(allFloats, i2f(i), byteArrayOf_u8(FCONST_0 + i))
        }

        int1AndInt2s.forEach { i ->
            add(
                allFloats,
                i2f(i),
                concatenate(allInts[i]!!, i2fBytes)
            )
        }

        // hardcode unusual float values for simplicity
        add(allFloats, FLOAT_SIGN, byteArrayOf_u8u8(FCONST_0, FNEG)) // -0.0
        add(allFloats, FLOAT_NAN, byteArrayOf_u8u8u8(FCONST_0, FCONST_0, FDIV)) // NaN
        add(allFloats, FLOAT_INF, byteArrayOf_u8u8u8(FCONST_1, FCONST_0, FDIV)) // Inf
        add(
            allFloats,
            FLOAT_NINF,
            byteArrayOf_u8u8u8u8(FCONST_1, FNEG, FCONST_0, FDIV)
        ) // -Inf

        // double constants
        val allDoubles: MutableLongObjectMap<ByteArray> = MutableLongObjectMap(213)
        for (i in 0 until 2) {
            add(allDoubles, i2d(i.toLong()), byteArrayOf_u8(DCONST_0 + i))
        }

        int1AndInt2s.forEach { i ->
            add(allDoubles, i2d(i.toLong()), concatenate(allInts[i]!!, i2dBytes))
        }

        add(allDoubles, DOUBLE_SIGN, byteArrayOf_u8u8(DCONST_0, DNEG)) // -0.0
        add(allDoubles, DOUBLE_NAN, byteArrayOf_u8u8u8(DCONST_0, DCONST_0, DDIV)) // NaN
        add(allDoubles, DOUBLE_INF, byteArrayOf_u8u8u8(DCONST_1, DCONST_0, DDIV)) // Inf
        add(allDoubles, DOUBLE_NINF, byteArrayOf_u8u8u8u8(DCONST_1, DNEG, DCONST_0, DDIV)) // -Inf

        assert(allInts.size == 65542)
        assert(allLongs.size == 256)
        assert(allFloats.size == 176)
        assert(allDoubles.size == 217)

        return LookupTables(allInts, allFloats, allLongs, allDoubles)
    }

    class LookupTables(
        @JvmField
        val allInts: IntObjectMap<ByteArray>,
        @JvmField
        val allFloats: IntObjectMap<ByteArray>,
        @JvmField
        val allLongs: LongObjectMap<ByteArray>,
        @JvmField
        val allDoubles: LongObjectMap<ByteArray>
    )
}
