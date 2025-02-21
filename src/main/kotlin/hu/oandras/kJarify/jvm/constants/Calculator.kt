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

import androidx.collection.MutableIntList
import hu.oandras.kJarify.*
import hu.oandras.kJarify.jvm.*
import hu.oandras.kJarify.jvm.JvmOps.DCONST_1
import hu.oandras.kJarify.jvm.JvmOps.DDIV
import hu.oandras.kJarify.jvm.JvmOps.DMUL
import hu.oandras.kJarify.jvm.JvmOps.FDIV
import hu.oandras.kJarify.jvm.JvmOps.FMUL
import hu.oandras.kJarify.jvm.JvmOps.I2L
import hu.oandras.kJarify.jvm.JvmOps.ICONST_M1
import hu.oandras.kJarify.jvm.JvmOps.ISHL
import hu.oandras.kJarify.jvm.JvmOps.L2D
import hu.oandras.kJarify.jvm.JvmOps.L2F
import hu.oandras.kJarify.jvm.JvmOps.LCONST_1
import hu.oandras.kJarify.jvm.JvmOps.LSHL
import hu.oandras.kJarify.jvm.JvmOps.LXOR
import hu.oandras.kJarify.jvm.constants.GenLookUp.DOUBLE_NAN
import hu.oandras.kJarify.jvm.constants.GenLookUp.DOUBLE_SIGN
import hu.oandras.kJarify.jvm.constants.GenLookUp.FLOAT_NAN
import hu.oandras.kJarify.jvm.constants.GenLookUp.FLOAT_SIGN
import kotlin.math.abs

@Suppress("PrivatePropertyName")
internal class Calculator {

    private val INTS: IntLookupTable
    private val FLOATS: IntLookupTable
    private val LONGS:  LongLookupTable
    private val DOUBLES: LongLookupTable

    init {
        val maps = GenLookUp.generate()
        INTS = maps.allInts
        FLOATS = maps.allFloats
        LONGS = maps.allLongs
        DOUBLES = maps.allDoubles
    }

    private fun calcInt(x: Int): ByteArray {
        return INTS.getOrElse(x) {
            calcIntImpl(x)
        }
    }

    private fun calcIntImpl(x: Int): ByteArray {
        val low = s16(x)
        val high = (x xor low) shr 16
        assert(high != 0)

        if (low == 0) {
            return concatenate(
                calcInt(high),
                calcInt(16),
                byteArrayOf_u8(ISHL)
            )
        }

        return concatenate(
            calcInt(high),
            calcInt(16),
            byteArrayOf_u8(ISHL),
            calcInt(low),
            IXOR_BYTES
        )
    }

    private fun calcLong(x: Long): ByteArray {
        return LONGS.getOrElse(x) {
            calcLongImpl(x)
        }
    }

    private fun calcLongImpl(x: Long): ByteArray {
        val low = x.toInt()
        val high = (x xor low.toLong()) shr 32
        if (high == 0L) {
            return concatenate(calcInt(low), I2L_BYTES)
        }

        var result = concatenate(calcInt(high.toInt()), byteArrayOf_u8(I2L), calcInt(32), byteArrayOf_u8(LSHL))
        if (low != 0) {
            result = concatenate(result, calcInt(low), byteArrayOf_u8u8(I2L, LXOR))
        }
        return result
    }

    private fun calcFloat(x: Int): ByteArray {
        assert(x == normalizeFloat(x))
        return FLOATS.getOrElse(x) {
            calcFloatImpl(x)
        }
    }

    private fun calcFloatImpl(x: Int): ByteArray {
        var exponent = ((x shr 23) and 0xFF) - 127
        var mantissa = (x.toUInt() % (1u shl 23)).toInt()
        if (exponent == -127) {
            exponent += 1
        } else {
            mantissa += 1 shl 23
        }
        exponent -= 23

        if ((x and FLOAT_SIGN) != 0) {
            mantissa = -mantissa
        }

        val exCombineOp = if (exponent < 0) FDIV else FMUL
        exponent = abs(exponent)
        val exponentParts = ArrayList<ByteArray>()
        while (exponent >= 63) {
            exponentParts.add(byteArrayOf_u8u8u8u8u8(LCONST_1, ICONST_M1, LSHL, L2F, exCombineOp))
            mantissa = -mantissa
            exponent -= 63
        }

        if (exponent > 0) {
            exponentParts.add(LCONST_1_BYTES)
            exponentParts.add(calcInt(exponent))
            exponentParts.add(byteArrayOf_u8u8u8(LSHL, L2F, exCombineOp))
        }

        exponentParts.add(0, calcInt(mantissa))
        exponentParts.add(1, I2F_BYTES)

        return concatenate(exponentParts)
    }

    private fun calcDouble(x: Long): ByteArray {
        assert(x == normalizeDouble(x))
        return DOUBLES.getOrElse(x) {
            calcDoubleImpl(x)
        }
    }

    private fun calcDoubleImpl(x: Long): ByteArray {
        // max required - 55 bytes
        var exponent = ((x shr 52).toInt() and 0x7FF) - 1023
        var mantissa = (x.toULong() % (1uL shl 52)).toLong()
        // check for denormals!
        if (exponent == -1023) {
            exponent += 1
        } else {
            mantissa += 1L shl 52
        }
        exponent -= 52

        if ((x and DOUBLE_SIGN) != 0L) {
            mantissa = -mantissa
        }

        val absExponent = abs(exponent)
        val exponentParts = ArrayList<ByteArray>()

        val part63 = absExponent / 63
        if (part63 > 0) {
            // create *63 part of exponent by repeated squaring
            if (exponent < 0) {
                exponentParts.add(byteArrayOf_u8u8u8u8u8u8(DCONST_1, LCONST_1, ICONST_M1, LSHL, L2D, DDIV))
            } else {
                exponentParts.add(byteArrayOf_u8u8u8u8(LCONST_1, ICONST_M1, LSHL, L2D))
            }

            if (part63 % 2 == 1) {
                mantissa = -mantissa
            }

            var lastNeeded = part63 % 2 == 1
            val until = Integer.SIZE - part63.countLeadingZeroBits()
            val stack = MutableIntList(until)
            stack.add(1) // Not actually required to compute the results - it's just used for a sanity check
            for (bi in 1 until until) {
                exponentParts.add(DUP2_BYTES)
                stack.add(stack.last())
                if (lastNeeded) {
                    exponentParts.add(DUP2_BYTES)
                    stack.add(stack.last())
                }
                exponentParts.add(DMUL_BYTES)
                stack.add(stack.removeAt(stack.lastIndex) + stack.removeAt(stack.lastIndex))
                lastNeeded = (part63 and (1 shl bi)) != 0
            }

            val part63OneBits = part63.countOneBits()

            assert(stack.sum() == part63 && part63OneBits == stack.size)

            exponentParts.add(ByteArray(part63OneBits) {
                DMUL.toByte()
            })
        }

        // now handle the rest
        val rest = absExponent % 63
        if (rest > 0) {
            exponentParts.add(LCONST_1_BYTES)
            exponentParts.add(calcInt(rest))
            exponentParts.add(byteArrayOf_u8u8u8(
                LSHL,
                L2D,
                if (exponent < 0) DDIV else DMUL
            ))
        }

        exponentParts.add(0, calcLong(mantissa))
        exponentParts.add(1, L2D_BYTES)

        return concatenate(exponentParts)
    }

    fun calc32(staticType: Int, value: Int): ByteArray {
        return when (staticType) {
            Scalars.INT -> calcInt(value)
            Scalars.FLOAT -> calcFloat(normalizeFloat(value))
            else -> throw AssertionError()
        }
    }

    fun calc64(staticType: Int, value: Long): ByteArray {
        return when (staticType) {
            Scalars.LONG -> calcLong(value)
            Scalars.DOUBLE -> calcDouble(normalizeDouble(value))
            else -> throw AssertionError()
        }
    }

    fun lookupOnly32(staticType: Int, value: Int): ByteArray? {
        // assume floats and double have already been normalized but int/longs haven't
        return when (staticType) {
            Scalars.INT -> INTS[value]
            Scalars.FLOAT -> FLOATS[value]
            else -> error("Invalid staticType: $staticType")
        }
    }

    fun lookupOnly64(staticType: Int, value: Long): ByteArray? {
        // assume floats and double have already been normalized but int/longs haven't
        return when (staticType) {
            Scalars.LONG -> LONGS[value]
            Scalars.DOUBLE -> DOUBLES[value]
            else -> error("Invalid staticType: $staticType")
        }
    }

    companion object {

        fun normalize(staticType: Int, value: Int): Int {
            return when (staticType) {
                Scalars.FLOAT -> normalizeFloat(value)
                Scalars.DOUBLE -> error("Invalid")
                else -> value
            }
        }

        fun normalize(staticType: Int, value: Long): Long {
            return when (staticType) {
                Scalars.FLOAT -> error("Invalid")
                Scalars.DOUBLE -> normalizeDouble(value)
                else -> value
            }
        }

        internal fun normalizeFloat(x: Int): Int {
            return if (Float.fromBits(x).isNaN()) {
                FLOAT_NAN
            } else {
                x
            }
        }

        internal fun normalizeDouble(x: Long): Long {
            return if (Double.fromBits(x).isNaN()) {
                DOUBLE_NAN
            } else {
                x
            }
        }
    }
}
