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

@file:Suppress("SpellCheckingInspection", "UNCHECKED_CAST")

package hu.oandras.kJarify.dex

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import hu.oandras.kJarify.dex.DalvikInstructionParameter.*
import hu.oandras.kJarify.keysToRanges
import kotlin.math.min

internal object InstructionDecoder {

    private val INSTRUCTION_FORMAT: IntObjectMap<String> = keysToRanges(
        MutableIntObjectMap<String>().apply {
            put(0x00, "10x")
            put(0x01, "12x")
            put(0x02, "22x")
            put(0x03, "32x")
            put(0x04, "12x")
            put(0x05, "22x")
            put(0x06, "32x")
            put(0x07, "12x")
            put(0x08, "22x")
            put(0x09, "32x")
            put(0x0a, "11x")
            put(0x0b, "11x")
            put(0x0c, "11x")
            put(0x0d, "11x")
            put(0x0e, "10x")
            put(0x0f, "11x")
            put(0x10, "11x")
            put(0x11, "11x")
            put(0x12, "11n")
            put(0x13, "21s")
            put(0x14, "31i")
            put(0x15, "21h")
            put(0x16, "21s")
            put(0x17, "31i")
            put(0x18, "51l")
            put(0x19, "21h")
            put(0x1a, "21c")
            put(0x1b, "31c")
            put(0x1c, "21c")
            put(0x1d, "11x")
            put(0x1e, "11x")
            put(0x1f, "21c")
            put(0x20, "22c")
            put(0x21, "12x")
            put(0x22, "21c")
            put(0x23, "22c")
            put(0x24, "35c")
            put(0x25, "3rc")
            put(0x26, "31t")
            put(0x27, "11x")
            put(0x28, "10t")
            put(0x29, "20t")
            put(0x2a, "30t")
            put(0x2b, "31t")
            put(0x2c, "31t")
            put(0x2d, "23x")
            put(0x32, "22t")
            put(0x38, "21t")
            put(0x3e, "10x")
            put(0x44, "23x")
            put(0x52, "22c")
            put(0x60, "21c")
            put(0x6e, "35c")
            put(0x73, "10x")
            put(0x74, "3rc")
            put(0x79, "10x")
            put(0x7b, "12x")
            put(0x90, "23x")
            put(0xb0, "12x")
            put(0xd0, "22s")
            put(0xd8, "22b")
            put(0xe3, "10x")
        }, 256
    )

    private val EmptyArray: Array<DalvikInstructionParameter> = emptyArray()

    private val integerParams: Array<IntegerParam> = Array<IntegerParam>(256) {
        IntegerParam(it)
    }

    private fun integerParamOf(value: Int): IntegerParam {
        if (value in integerParams.indices) {
            return integerParams[value]
        }

        return IntegerParam(value)
    }

    // parsing funcs
    private fun p00op(): Array<DalvikInstructionParameter> {
        return EmptyArray
    }

    private fun pBAop(w: Int): Array<DalvikInstructionParameter> {
        return arrayOf(
            integerParamOf((w shr 8) and 0xF),
            integerParamOf(w shr 12)
        )
    }

    private fun pAAop(w: Int): Array<DalvikInstructionParameter> {
        return arrayOf(
            integerParamOf(w shr 8)
        )
    }

    @Suppress("unused")
    private fun p00opAAAA(w: Int, w2: Int): Array<DalvikInstructionParameter> {
        return arrayOf(
            integerParamOf(w2)
        )
    }

    private fun pAAopBBBB(w: Int, w2: Int): Array<DalvikInstructionParameter> {
        return arrayOf(
            integerParamOf(w shr 8),
            integerParamOf(w2)
        )
    }

    private fun pAAopCCBB(w: Int, w2: Int): Array<DalvikInstructionParameter> {
        return arrayOf(
            integerParamOf(w shr 8),
            integerParamOf(w2 and 0xFF),
            integerParamOf(w2 shr 8)
        )
    }

    private fun pBAopCCCC(w: Int, w2: Int): Array<DalvikInstructionParameter> {
        return arrayOf(
            integerParamOf(w shr 8 and 0xF),
            integerParamOf(w shr 12),
            integerParamOf(w2)
        )
    }

    private fun p00opAAAAAAAA(w: Int, w2: Int, w3: Int): Array<DalvikInstructionParameter> {
        return arrayOf(
            integerParamOf(w2 xor (w3 shl 16))
        )
    }

    private fun p00opAAAABBBB(w: Int, w2: Int, w3: Int): Array<DalvikInstructionParameter> {
        return arrayOf(
            integerParamOf(w2),
            integerParamOf(w3),
        )
    }

    private fun pAAopBBBBBBBB(w: Int, w2: Int, w3: Int): Array<DalvikInstructionParameter> {
        return arrayOf(
            integerParamOf(w shr 8),
            integerParamOf(w2 xor (w3 shl 16))
        )
    }

    private fun pAGopBBBBFEDC(w: Int, w2: Int, w3: Int): Array<DalvikInstructionParameter> {
        val a = w shr 12

        val c = w3 and 0xF
        val d = (w3 shr 4) and 0xF
        val e = (w3 shr 8) and 0xF
        val f = (w3 shr 12) and 0xF
        val g = (w shr 8) and 0xF

        val params2 = IntArray(min(a, 5))

        val size = params2.size
        if (size > 0) {
            params2[0] = c
        }
        if (size > 1) {
            params2[1] = d
        }
        if (size > 2) {
            params2[2] = e
        }
        if (size > 3) {
            params2[3] = f
        }
        if (size > 4) {
            params2[4] = g
        }

        return arrayOf(
            integerParamOf(w2),
            IntArrayParam(params2),
        )
    }

    private fun pAAopBBBBCCCC(w: Int, w2: Int, w3: Int): Array<DalvikInstructionParameter> {
        val a = w shr 8
        return arrayOf(
            integerParamOf(w2),
            IntArrayParam(
                IntArray(a) {
                    w3 + it
                }
            )
        )
    }

    private fun pAAopBBBBBBBBBBBBBBBB(w: Int, w2: Int, w3: Int, w4: Int, w5: Int): Array<DalvikInstructionParameter> {
        return arrayOf(
            integerParamOf(w shr 8),
            LongParam(
                w2.toUInt().toLong() xor (w3.toUInt().toLong() shl 16) xor (w4.toUInt()
                    .toLong() shl 32) xor (w5.toUInt().toLong() shl 48)
            ),
        )
    }

    private fun mapFunctions(fmt: String, w: IntArray, pos: Int): Array<DalvikInstructionParameter> {
        val w0 = w[pos]

        return when (fmt) {
            "10x" -> p00op()
            "12x" -> pBAop(w0)
            "11n" -> pBAop(w0)
            "11x" -> pAAop(w0)
            "10t" -> pAAop(w0)
            "20t" -> p00opAAAA(w0, w[pos + 1])
            "22x" -> pAAopBBBB(w0, w[pos + 1])
            "21t" -> pAAopBBBB(w0, w[pos + 1])
            "21s" -> pAAopBBBB(w0, w[pos + 1])
            "21h" -> pAAopBBBB(w0, w[pos + 1])
            "21c" -> pAAopBBBB(w0, w[pos + 1])
            "23x" -> pAAopCCBB(w0, w[pos + 1])
            "22b" -> pAAopCCBB(w0, w[pos + 1])
            "22t" -> pBAopCCCC(w0, w[pos + 1])
            "22s" -> pBAopCCCC(w0, w[pos + 1])
            "22c" -> pBAopCCCC(w0, w[pos + 1])
            "30t" -> p00opAAAAAAAA(w0, w[pos + 1], w[pos + 2])
            "32x" -> p00opAAAABBBB(w0, w[pos + 1], w[pos + 2])
            "31i" -> pAAopBBBBBBBB(w0, w[pos + 1], w[pos + 2])
            "31t" -> pAAopBBBBBBBB(w0, w[pos + 1], w[pos + 2])
            "31c" -> pAAopBBBBBBBB(w0, w[pos + 1], w[pos + 2])
            "35c" -> pAGopBBBBFEDC(w0, w[pos + 1], w[pos + 2])
            "3rc" -> pAAopBBBBCCCC(w0, w[pos + 1], w[pos + 2])
            "51l" -> pAAopBBBBBBBBBBBBBBBB(w0, w[pos + 1], w[pos + 2], w[pos + 3], w[pos + 4])
            else -> error("Unsupported!")
        }
    }

    private fun sign(param: IntegerParam, bits: Int): IntegerParam {
        return integerParamOf(sign(param.value, bits))
    }

    private fun sign(x: Int, bits: Int): Int {
        var x = x.toUInt()

        if (x >= (1u shl (bits - 1))) {
            x -= 1u shl bits
        }

        return x.toInt()
    }

    class DecodeResult(
        @JvmField
        val pos: Int,
        @JvmField
        val args: Array<DalvikInstructionParameter>,
    )

    @Suppress("DEPRECATION")
    fun decode(shorts: IntArray, pos: Int, opcode: Int): DecodeResult {
        val fmt: String = INSTRUCTION_FORMAT[opcode]!!
        val size = fmt[0].toInt() - '0'.toInt()
        val results: Array<DalvikInstructionParameter> = mapFunctions(fmt, shorts, pos)

        val lastIndex = results.size - 1

        when (fmt[2]) {
            'n' -> {
                results[lastIndex] = sign(results[lastIndex] as IntegerParam, 4)
            }

            'b' -> {
                results[lastIndex] = sign(results[lastIndex] as IntegerParam, 8)
            }

            's' -> {
                results[lastIndex] = sign(results[lastIndex] as IntegerParam, 16)
            }

            't' -> when (size) {
                1 -> {
                    results[lastIndex] = sign(results[lastIndex] as IntegerParam, 8)
                }

                2 -> {
                    results[lastIndex] = sign(results[lastIndex] as IntegerParam, 16)
                }
            }
        }

        // Hats depend on actual size expected, so we rely on opcode as a hack
        if (fmt[2] == 'h') {
            assert(opcode == 0x15 || opcode == 0x19)
            val param = results[lastIndex] as IntegerParam
            if (opcode == 0x15) {
                results[lastIndex] = integerParamOf(param.value shl 16)
            } else {
                results[lastIndex] = LongParam(param.value.toUInt().toLong() shl 48)
            }
        }

        // Convert code offsets to actual code position
        if (fmt[2] == 't') {
            results[lastIndex] = integerParamOf((results[lastIndex] as IntegerParam).value + pos)
        }

        return DecodeResult(pos + size, results)
    }
}