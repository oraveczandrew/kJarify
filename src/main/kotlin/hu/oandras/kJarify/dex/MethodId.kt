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

import hu.oandras.kJarify.equalsWithChar
import hu.oandras.kJarify.streams.Stream

@JvmField
internal val EmptySpacedParamArray: Array<ByteArray?> = arrayOfNulls(0)

class MethodId(
    dex: DexFile,
    methodIdx: Int
) : MFIdMixin() {

    @JvmField
    val returnType: ByteArray

    @JvmField
    val paramTypes: Array<ByteArray>

    override val className: ByteArray
    override val name: ByteArray
    override val descriptor: ByteArray

    init {
        val stream: Stream = dex.stream(dex.methodIds.offset + methodIdx * 8)
        this.className = dex.classType(stream.u16())
        val protoIdx: Int = stream.u16()
        this.name = dex.string(stream.u32())

        val stream2: Stream = dex.stream(dex.protoIds.offset + protoIdx * 12)
        @Suppress("unused")
        val shortyIdx: Int = stream2.u32()
        val returnIndex: Int = stream2.u32()
        val parametersOff: Int = stream2.u32()
        this.returnType = dex.type(returnIndex)
        this.paramTypes = dex.typeList(parametersOff, false)

        // rearrange things to Java format
        this.descriptor = buildDescriptor(paramTypes, returnType)
    }

    @Suppress("DEPRECATION")
    private fun buildDescriptor(paramTypes: Array<ByteArray>, returnType: ByteArray): ByteArray {
        val sum = 2 + paramTypes.sumOf { it.size } + returnType.size

        val arr = ByteArray(sum)
        arr[0] = '('.toInt().toByte()

        var filled = 1
        for (paramType in paramTypes) {
            paramType.copyInto(arr, filled)
            filled += paramType.size
        }

        arr[filled] = ')'.toInt().toByte()
        filled++

        returnType.copyInto(arr, filled)

        return arr
    }

    private var spacedParamTypes: Array<ByteArray?>? = null
    private var isStatic = false

    @Suppress("DEPRECATION")
    fun getSpacedParameterTypes(isStatic: Boolean): Array<ByteArray?> {
        val spacedParamTypes = spacedParamTypes
        if (spacedParamTypes != null) {
            assert(isStatic == this.isStatic)
            return spacedParamTypes
        }

        val results = ArrayList<ByteArray?>(1 + paramTypes.size)

        if (!isStatic) {
            val cname = className
            if (cname[0] == '['.toInt().toByte()) {
                results.add(cname)
            } else {
                results.add(ByteArray(cname.size + 2).apply {
                    this[0] = 'L'.toInt().toByte()
                    cname.copyInto(this, 1)
                    this[lastIndex] = ';'.toInt().toByte()
                })
            }
        }

        for (ptype in paramTypes) {
            results.add(ptype)
            if (ptype.equalsWithChar('J') || ptype.equalsWithChar('D')) {
                results.add(null)
            }
        }

        val arr = if (results.isEmpty()) {
            EmptySpacedParamArray
        } else {
            results.toTypedArray()
        }
        this.spacedParamTypes = arr
        this.isStatic = isStatic
        return arr
    }

    override fun toString(): String {
        return buildString {
            append(className.decodeToString())
            append('.')
            append(name.decodeToString())
            append('(')
            paramTypes.joinTo(this, "") { it.decodeToString() }
            append("): ")
            append(returnType.decodeToString())
        }
    }
}