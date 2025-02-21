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

import hu.oandras.kJarify.ThreadSafeIntObjectMap
import hu.oandras.kJarify.streams.Reader
import hu.oandras.kJarify.streams.Stream

const val NO_INDEX: Int = -1

class DexFile(
    private val raw: ByteArray
) {

    private val stream: Reader = Reader(raw)
    private val link: SizeOff
    private val mapOffset: Int
    private val stringIds: SizeOff
    private val typeIds: SizeOff
    @JvmField
    internal val protoIds: SizeOff
    @JvmField
    internal val fieldIds: SizeOff
    @JvmField
    internal val methodIds: SizeOff
    private val classDefinitions: SizeOff

    @JvmField
    val classes: List<DexClass>

    private val stringCache: ThreadSafeIntObjectMap<ByteArray> = ThreadSafeIntObjectMap()
    private val classTypeCache: ThreadSafeIntObjectMap<ByteArray> = ThreadSafeIntObjectMap()
    private val fieldIdCache: ThreadSafeIntObjectMap<FieldId> = ThreadSafeIntObjectMap()
    private val methodIdCache: ThreadSafeIntObjectMap<MethodId> = ThreadSafeIntObjectMap()

    init {
        // parse header
        stream.read(36)
        if (stream.u32() != 0x70) {
            println("Warning, unexpected header size!")
        }
        if (stream.u32() != 0x12345678) {
            println("Warning, unexpected endianness tag!")
        }

        this.link = SizeOff(stream.u32(), stream.u32())
        this.mapOffset = stream.u32()
        this.stringIds = SizeOff(stream.u32(), stream.u32())
        this.typeIds = SizeOff(stream.u32(), stream.u32())
        this.protoIds = SizeOff(stream.u32(), stream.u32())
        this.fieldIds = SizeOff(stream.u32(), stream.u32())
        this.methodIds = SizeOff(stream.u32(), stream.u32())
        this.classDefinitions = SizeOff(stream.u32(), stream.u32())

        val defsSize: Int = classDefinitions.size
        val classes = ArrayList<DexClass>(defsSize)
        for (i in 0 until defsSize) {
            classes.add(DexClass(this, classDefinitions.offset, i))
        }
        this.classes = classes
    }

    internal fun stream(offset: Int): Reader {
        return Reader(raw, offset)
    }

    fun string(i: Int): ByteArray {
        return stringCache.getOrPut(
            key = i,
            isInterruptible = true,
            atomicCreate = false,
        ) {
            val dataOffset = stream(stringIds.offset + i * 4).u32()
            val stream = stream(dataOffset)
            stream.unsignedLeb128() // ignore decoded length
            return stream.readClassString()
        }
    }

    fun type(i: Int): ByteArray {
        var uI = i.toUInt()
        if (0u <= uI && uI < NO_INDEX.toUInt()) {
            val strIndex = stream(typeIds.offset + i * 4).u32()
            return string(strIndex)
        }
        return error("Unsupported type!")
    }

    @Suppress("DEPRECATION")
    fun classType(i: Int): ByteArray {
        // Can be either class _name_ or array _descriptor_
        return classTypeCache.getOrPut(
            key = i,
            isInterruptible = true,
            atomicCreate = false,
        ) {
            val desc = type(i)

            when {
                desc[0] == '['.toInt().toByte() -> {
                    desc
                }

                desc[0] == 'L'.toInt().toByte() -> {
                    desc.copyOfRange(1, desc.size - 1)
                }

                else -> {
                    // Not sure how to handle primitive classes properly,
                    // but this should hopefully be good enough.
                    desc
                }
            }
        }
    }

    fun fieldId(i: Int): FieldId {
        return fieldIdCache.getOrPut(
            key = i,
            isInterruptible = true,
            atomicCreate = false,
        ) {
            FieldId(this, i)
        }
    }

    fun methodId(i: Int): MethodId {
        return methodIdCache.getOrPut(
            key = i,
            isInterruptible = true,
            atomicCreate = false,
        ) {
            MethodId(this, i)
        }
    }

    fun typeList(off: Int, parseClsDesc: Boolean): Array<ByteArray> {
        if (off == 0) {
            return emptyArray()
        }

        val st: Stream = stream(off)

        val size: Int = st.u32()
        val indexes = IntArray(size) {
            st.u16()
        }

        return if (parseClsDesc) {
            Array<ByteArray>(indexes.size) {
                classType(indexes[it])
            }
        } else {
            Array<ByteArray>(indexes.size) {
                type(indexes[it])
            }
        }
    }
}