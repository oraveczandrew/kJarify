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

import hu.oandras.kJarify.dex.ConstantValue.*
import hu.oandras.kJarify.signExtend
import hu.oandras.kJarify.streams.Stream

class DexClass(
    private val dex: DexFile,
    baseOff: Int,
    i: Int
) {

    @JvmField
    val name: ByteArray
    @JvmField
    val access: Int
    @JvmField
    val superClass: ByteArray?
    @JvmField
    val interfaces: Array<ByteArray>
    @JvmField
    val dataOffset: Int
    // parse data lazily in parseData()
    @JvmField
    var data: ClassData? = null
    @JvmField
    val constantValuesOffset: Int

    init {
        val st: Stream = dex.stream(baseOff + i * 32)

        this.name = dex.classType(st.u32())
        this.access = st.u32()
        val superClassRef: Int = st.u32()
        this.superClass = if (superClassRef != NO_INDEX) dex.classType(superClassRef) else null
        this.interfaces = dex.typeList(st.u32(), true)
        st.u32()
        st.u32()
        this.dataOffset = st.u32()
        this.constantValuesOffset = st.u32()
    }

    fun parseData() {
        if (data == null) {
            data = ClassData(dex, dataOffset).also {

                if (constantValuesOffset != 0) {
                    val stream: Stream = dex.stream(constantValuesOffset)
                    val fields = it.fields
                    for (j in 0 until stream.unsignedLeb128()) {
                        fields[j].constantValue = encodedValue(dex, stream)
                    }
                }
            }
        }
    }

    override fun toString(): String {
        return buildString {
            append("DexClass(name=")
            append(name.decodeToString())
            append(", access=")
            append(access)
            append(", superClass=")
            append(superClass?.decodeToString() ?: "null")
            append(", interfaces=")
            interfaces.joinTo(this, ", ") { it.decodeToString() }
            append(", dataOffset=")
            append(dataOffset)
            append(", constantValuesOffset=")
            append(constantValuesOffset)
            append(")")
        }
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        internal fun encodedValue(dex: DexFile, stream: Stream): ConstantValue? {
            val tag: Int = stream.u8()
            val valueType = tag and 31
            val valueArgument = tag shr 5

            if (valueType == 0x1c) { // ARRAY
                val size: Int = stream.unsignedLeb128()
                val array = Array<ConstantValue?>(size) {
                    encodedValue(dex, stream)
                }
                return ConstantValueArrayConstant(array)
            }

            if (valueType == 0x1d) { // ANNOTATION
                stream.unsignedLeb128()
                @Suppress("unused")
                for (i in 0 until stream.unsignedLeb128()) {
                    stream.unsignedLeb128()
                    encodedValue(dex, stream)
                }
                return null
            }

            if (valueType == 0x1e) { // NULL
                return null
            }

            // For the rest, we just return it as unsigned integers without recording type
            // extended to either u32 or u64 depending on int/float or long/double
            if (valueType == 0x1f) { // BOOLEAN
                return IntConstant('I', valueArgument)
            }

            // the rest are an int encoded into varg + 1 bytes in some way
            val size = valueArgument + 1
            var value: Long = 0
            for (i in 0 until size) {
                value += stream.u8().toLong() shl (i * 8)
            }

            return when (valueType) {
                0x00 -> { // BYTE
                    IntConstant('I', signExtend(value.toInt(), 8))
                }
                0x02 -> { // SHORT
                    IntConstant('I', signExtend(value.toInt(), 16))
                }
                0x03 -> { // CHAR
                    IntConstant('I', value.toInt())
                }
                0x04 -> { // INT
                    IntConstant('I', value.toInt())
                }
                0x06 -> { // LONG
                    LongConstant('J', value)
                }
                // floats are 0 extended to the right
                0x10 -> { // FLOAT
                    IntConstant('F', value.toULong().toInt() shl (32 - size * 8))
                }
                0x11 -> { // DOUBLE
                    LongConstant('D', value shl (64 - size * 8))
                }
                0x17 -> { // STRING
                    ByteArrayConstant("Ljava/lang/String;", dex.string(value.toInt()))
                }
                0x18 -> { // TYPE
                    ByteArrayConstant("Ljava/lang/Class;", dex.classType(value.toInt()))
                }
                else -> {
                    null
                }
            }
        }
    }
}

