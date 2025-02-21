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

import androidx.collection.MutableObjectIntMap
import hu.oandras.kJarify.byteArrayOf_u8u8u8
import hu.oandras.kJarify.dex.FieldId
import hu.oandras.kJarify.dex.MFIdMixin
import hu.oandras.kJarify.dex.MethodId
import hu.oandras.kJarify.forEachElement
import hu.oandras.kJarify.streams.Writer
import java.io.IOException

internal sealed interface PoolData {

    val tag: Int

    data class IntData(
        override val tag: Int,
        val value: Int
    ) : PoolData

    data class LongData(
        override val tag: Int,
        val value: Long,
    ) : PoolData

    class IntArrayData(
        override val tag: Int,
        val intArray: IntArray,
    ) : PoolData {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as IntArrayData

            if (tag != other.tag) return false
            if (!intArray.contentEquals(other.intArray)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = tag
            result = 31 * result + intArray.contentHashCode()
            return result
        }

        override fun toString(): String {
            return "IntArrayData(tag=$tag, intArray=${intArray.contentToString()})"
        }
    }

    class ByteArrayData(
        override val tag: Int,
        val byteArray: ByteArray,
    ) : PoolData {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ByteArrayData

            if (tag != other.tag) return false
            if (!byteArray.contentEquals(other.byteArray)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = tag
            result = 31 * result + byteArray.contentHashCode()
            return result
        }

        override fun toString(): String {
            return "ByteArrayData(tag=$tag, byteArray='${byteArray.decodeToString()}')"
        }
    }
}

@Suppress("ConstPropertyName", "SpellCheckingInspection", "PropertyName")
internal abstract class ConstantPool {

    private val lookup: Array<MutableObjectIntMap<PoolData>> = Array(MAX_CONST + 1) {
        MutableObjectIntMap()
    }

    @JvmField
    protected val _values: ArrayList<PoolData?> = ArrayList()

    val values: List<PoolData?>
        get() = _values

    @Throws(ClassFileLimitExceeded::class)
    protected abstract fun obtainIndex(low: Boolean, width: Int): Int

    abstract fun space(): Int

    abstract fun lowSpace(): Int

    @Throws(ClassFileLimitExceeded::class)
    private fun get(tag: Int, constantData: PoolData): Int {
        return lookup[tag].getOrPut(constantData) {
            val low = tag == CONSTANT_Integer || tag == CONSTANT_Float || tag == CONSTANT_String
            val index = obtainIndex(low, width(tag))
            set(
                constantData,
                index
            )
            index
        }
    }

    private fun set(
        constantData: PoolData,
        index: Int,
    ) {
        val d = lookup[constantData.tag]
        d.put(constantData, index)
        assert(_values.set(index, constantData) == null)
    }

    private fun get(tag: Int, args: Int): Int {
        return get(tag, PoolData.IntData(tag, args))
    }

    private fun get(tag: Int, args: Long): Int {
        return get(tag, PoolData.LongData(tag, args))
    }

    private fun get(tag: Int, args: IntArray): Int {
        return get(tag, PoolData.IntArrayData(tag, args))
    }

    @Suppress("SameParameterValue")
    private fun get(tag: Int, args: ByteArray): Int {
        return get(tag, PoolData.ByteArrayData(tag, args))
    }

    @Throws(ClassFileLimitExceeded::class)
    fun insertDirectly(contantData: PoolData, low: Boolean) {
        val index = obtainIndex(low, width(contantData.tag))
        set(
            contantData,
            index,
        )
    }

    @Throws(ClassFileLimitExceeded::class)
    fun dataRef(contantData: PoolData): Int {
        val tag = contantData.tag
        val d = lookup[tag]
        val argRef = contantData
        if (d.containsKey(argRef)) {
            return d[argRef]
        }
        val width: Int = width(tag)
        if (width > space()) {
            return -1
        }
        val index = obtainIndex(true, width)
        set(
            contantData,
            index,
        )
        return index
    }

    @Throws(ClassFileLimitExceeded::class)
    fun utf8Ref(s: ByteArray): Int {
        return get(CONSTANT_Utf8, s)
    }

    @Throws(ClassFileLimitExceeded::class)
    fun classRef(s: ByteArray): Int {
        return get(CONSTANT_Class, utf8Ref(s))
    }

    @Throws(ClassFileLimitExceeded::class)
    fun stringRef(s: ByteArray): Int {
        return get(CONSTANT_String, utf8Ref(s))
    }

    @Throws(ClassFileLimitExceeded::class)
    fun namedArrayRef(name: ByteArray, desc: ByteArray): Int {
        return get(CONSTANT_NameAndType, intArrayOf(utf8Ref(name), utf8Ref(desc)))
    }

    @Throws(ClassFileLimitExceeded::class)
    private fun mixin(tag: Int, triple: MFIdMixin): Int {
        return get(
            tag,
            intArrayOf(classRef(triple.className), namedArrayRef(triple.name, triple.descriptor))
        )
    }

    @Throws(ClassFileLimitExceeded::class)
    fun fieldRef(fieldId: FieldId): Int {
        return mixin(CONSTANT_Fieldref, fieldId)
    }

    @Throws(ClassFileLimitExceeded::class)
    fun methodRef(methodId: MethodId): Int {
        return mixin(CONSTANT_Methodref, methodId)
    }

    fun interfaceMethodRef(trip: MethodId): Int {
        return mixin(CONSTANT_InterfaceMethodref, trip)
    }

    fun intRef(x: Int): Int {
        return get(CONSTANT_Integer, x)
    }

    fun floatRef(x: Int): Int {
        return get(CONSTANT_Float, x)
    }

    fun longRef(x: Long): Int {
        return get(CONSTANT_Long, x)
    }

    fun doubleRef(x: Long): Int {
        return get(CONSTANT_Double, x)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun writeEntry(stream: Writer, item: PoolData?) {
        if (item == null) {
            return
        }

        val tag = item.tag
        stream.u8(tag)

        when (tag) {
            CONSTANT_Utf8 -> {
                val value = (item as PoolData.ByteArrayData).byteArray
                stream.u16(value.size)
                stream.write(value)
            }
            CONSTANT_Integer,
            CONSTANT_Float -> {
                val value = (item as PoolData.IntData).value
                stream.u32(value)
            }
            CONSTANT_Long,
            CONSTANT_Double -> {
                val value = (item as PoolData.LongData).value
                stream.u64(value)
            }
            CONSTANT_Class,
            CONSTANT_String -> {
                val value = (item as PoolData.IntData).value
                stream.u16(value)
            }
            CONSTANT_Methodref,
            CONSTANT_NameAndType,
            CONSTANT_Fieldref,
            CONSTANT_InterfaceMethodref -> {
                val value = (item as PoolData.IntArrayData).intArray
                stream.u16(value[0])
                stream.u16(value[1])
            }
            else -> {
                error("unsupported tag: $tag")
            }
        }
    }

    abstract fun write(stream: Writer)

    companion object {
        const val CONSTANT_Utf8: Int = 1
        const val CONSTANT_Integer: Int = 3
        const val CONSTANT_Float: Int = 4
        const val CONSTANT_Long: Int = 5
        const val CONSTANT_Double: Int = 6
        const val CONSTANT_Class: Int = 7
        const val CONSTANT_String: Int = 8
        const val CONSTANT_Fieldref: Int = 9
        const val CONSTANT_Methodref: Int = 10
        const val CONSTANT_InterfaceMethodref: Int = 11
        const val CONSTANT_NameAndType: Int = 12

        // public static final int CONSTANT_MethodHandle = 15;
        // public static final int CONSTANT_MethodType = 16;
        // public static final int CONSTANT_InvokeDynamic = 18;
        const val MAX_CONST: Int = CONSTANT_NameAndType

        fun width(tag: Int): Int {
            return if (tag == CONSTANT_Long || tag == CONSTANT_Double) 2 else 1
        }
    }
}

internal class SimpleConstantPool : ConstantPool() {

    init {
        _values.add(null)
    }

    override fun space(): Int {
        return 65535 - values.size
    }

    override fun lowSpace(): Int {
        return 256 - values.size
    }

    @Throws(ClassFileLimitExceeded::class)
    override fun obtainIndex(low: Boolean, width: Int): Int {
        if (space() < width) {
            throw ClassFileLimitExceeded()
        }
        val values = _values
        val temp = values.size
        values.ensureCapacity(temp + width)
        @Suppress("unused")
        for (i in 0 until width) {
            values.add(null)
        }
        return temp
    }

    @Throws(IOException::class)
    override fun write(stream: Writer) {
        stream.u16(values.size)
        values.forEachElement { item ->
            writeEntry(stream, item)
        }
    }
}

internal class SplitConstantPool : ConstantPool() {
    private var bottom = 1
    private var top: Int

    init {
        _values.ensureCapacity(POOL_SIZE)
        @Suppress("unused")
        for (i in 0 until POOL_SIZE) {
            _values.add(null)
        }
        top = POOL_SIZE
    }

    override fun space(): Int {
        return top - bottom
    }

    override fun lowSpace(): Int {
        return 256 - bottom
    }

    @Throws(ClassFileLimitExceeded::class)
    override fun obtainIndex(low: Boolean, width: Int): Int {
        if (space() < width) {
            throw ClassFileLimitExceeded()
        }

        if (low) {
            val index = bottom
            bottom += width
            return index
        }

        val newTop = top - width
        top = newTop
        return newTop
    }

    @Throws(IOException::class)
    override fun write(stream: Writer) {
        assert(65535 == values.size)

        stream.u16(values.size)

        assert(bottom <= top)

        var k = 0
        @Suppress("unused")
        for (i in 0 until bottom) {
            writeEntry(stream, values[k])
            k++
        }

        @Suppress("unused")
        for (i in 0 until space()) {
            stream.write(PLACEHOLDER_ENTRY)
        }

        for (i in top until values.size) {
            writeEntry(stream, values[i])
        }
    }

    companion object {
        private const val POOL_SIZE = 65535
        private val PLACEHOLDER_ENTRY: ByteArray = byteArrayOf_u8u8u8(CONSTANT_Utf8, 0, 0)
    }
}

