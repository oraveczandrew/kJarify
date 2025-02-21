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

package hu.oandras.kJarify.dex

import androidx.collection.IntIntMap
import hu.oandras.kJarify.dex.DalvikInstructionParameter.*

class DalvikInstruction internal constructor(
    @JvmField
    val type: DalvikOpcode,
    @JvmField
    val position: Int,
    @JvmField
    val pos2: Int,
    @JvmField
    val opcode: Int,
    private val args: Array<DalvikInstructionParameter>,
    @JvmField
    val switchData: IntIntMap?,
    @JvmField
    val fillArrData: ArrayData?,
) {
    @JvmField
    var implicitCasts: ImplicitCasts? = null

    @JvmField
    var prevResult: ByteArray? = null // for move-result/exception

    fun getArgsSize(): Int {
        return args.size
    }

    fun getIntArg(argIndex: Int): Int {
        return (args[argIndex] as IntegerParam).value
    }

    fun getLongArg(argIndex: Int): Long {
        val param = args[argIndex]
        if (param is IntegerParam) {
            return param.value.toLong()
        }
        return (param as LongParam).value
    }

    fun getIntArrayArg(argIndex: Int): IntArray {
        return (args[argIndex] as IntArrayParam).value
    }

    fun getLongArrayArg(argIndex: Int): LongArray {
        return (args[argIndex] as LongArrayParam).value
    }

    override fun toString(): String {
        return buildString {
            append("DalvikInstruction(type=")
            append(type)
            append(", opcode=")
            append(opcode)
            append(", params: [")
            args.forEach {
                append(it.toString())
                append(", ")
            }
            if (args.isNotEmpty()) {
                setLength(length -2)
            }
            append("], pos=")
            append(position)
            append(", pos2=")
            append(pos2)
            append("})")
        }
    }
}
