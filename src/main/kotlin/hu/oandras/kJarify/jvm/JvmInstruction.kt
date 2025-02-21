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

@file:Suppress("FunctionName")

package hu.oandras.kJarify.jvm

import androidx.collection.*
import hu.oandras.kJarify.*
import hu.oandras.kJarify.jvm.JvmOps.ATHROW
import hu.oandras.kJarify.jvm.JvmOps.GOTO
import hu.oandras.kJarify.jvm.JvmOps.GOTO_W
import hu.oandras.kJarify.jvm.JvmOps.IFEQ
import hu.oandras.kJarify.jvm.JvmOps.IFGE
import hu.oandras.kJarify.jvm.JvmOps.IFGT
import hu.oandras.kJarify.jvm.JvmOps.IFLE
import hu.oandras.kJarify.jvm.JvmOps.IFLT
import hu.oandras.kJarify.jvm.JvmOps.IFNE
import hu.oandras.kJarify.jvm.JvmOps.IFNONNULL
import hu.oandras.kJarify.jvm.JvmOps.IFNULL
import hu.oandras.kJarify.jvm.JvmOps.IF_ACMPEQ
import hu.oandras.kJarify.jvm.JvmOps.IF_ACMPNE
import hu.oandras.kJarify.jvm.JvmOps.IF_ICMPEQ
import hu.oandras.kJarify.jvm.JvmOps.IF_ICMPGE
import hu.oandras.kJarify.jvm.JvmOps.IF_ICMPGT
import hu.oandras.kJarify.jvm.JvmOps.IF_ICMPLE
import hu.oandras.kJarify.jvm.JvmOps.IF_ICMPLT
import hu.oandras.kJarify.jvm.JvmOps.IF_ICMPNE
import hu.oandras.kJarify.jvm.JvmOps.ILOAD
import hu.oandras.kJarify.jvm.JvmOps.ILOAD_0
import hu.oandras.kJarify.jvm.JvmOps.IRETURN
import hu.oandras.kJarify.jvm.JvmOps.LDC
import hu.oandras.kJarify.jvm.JvmOps.LDC2_W
import hu.oandras.kJarify.jvm.JvmOps.LDC_W
import hu.oandras.kJarify.jvm.JvmOps.LOOKUPSWITCH
import hu.oandras.kJarify.jvm.JvmOps.RETURN
import hu.oandras.kJarify.jvm.JvmOps.TABLESWITCH
import hu.oandras.kJarify.jvm.JvmOps.WIDE
import hu.oandras.kJarify.jvm.Scalars.staticTypeToStr
import hu.oandras.kJarify.jvm.constants.Calculator
import hu.oandras.kJarify.streams.withWriter

abstract class JvmInstruction {

    open val bytecode: ByteArray?
        get() = null

    open val min: Int
        get() = 0

    open val max: Int
        get() = 0

    val opCodeStr: String
        get() = bytecode?.let { JvmOps.opToStr(it[0]) } ?: "null"

    open fun fallsThrough(): Boolean = true

    open fun targets(): IntArray = EmptyIntArray

    open fun calculateBytecode(positionMap: ObjectIntMap<JvmInstruction>, labels: IntObjectMap<out JvmInstruction>) {
        error("Unsupported")
    }

    // To the correct execution this method can't be overridden
    final override fun hashCode(): Int = super.hashCode()

    // To the correct execution this method can't be overridden
    final override fun equals(other: Any?): Boolean = super.equals(other)

    @OptIn(ExperimentalStdlibApi::class)
    protected fun appendByteCodeStr(stringBuilder: StringBuilder) {
        val bytecode = bytecode
        if (bytecode != null) {
            bytecode.joinTo(stringBuilder, ", ") { it.toHexString() }
        } else {
            stringBuilder.append("null")
        }
    }

    class Label(
        // null or Integer
        @JvmField
        val id: Int? = null,
    ) : JvmInstruction() {
        override val bytecode: ByteArray
            get() = EmptyByteArray

        override fun toString(): String {
            return "Label(id=$id)"
        }
    }

    class RegistryAccess(
        registryId: Int,
        staticType: Int,
        @JvmField
        val store: Boolean
    ) : JvmInstruction() {

        @JvmField
        val key: Key = Key(registryId, staticType)

        @JvmField
        val wide: Boolean = Scalars.isWide(staticType)

        override var bytecode: ByteArray? = null

        fun calculateBytecode(local: Int) {
            assert(bytecode == null)
            val staticType: Int = key.staticType
            val opCodeOffset: Int = (JvmOps.ISTORE - ILOAD) * if (store) 1 else 0

            bytecode = when {
                local < 4 -> {
                    byteArrayOf_u8(
                        ILOAD_0 + opCodeOffset + local + ilfdaOrd(staticType) * 4
                    )
                }
                local < 256 -> {
                    byteArrayOf_u8u8(
                        ILOAD + opCodeOffset + ilfdaOrd(staticType),
                        local
                    )
                }
                else -> {
                    byteArrayOf_u8u8u16(
                        WIDE,
                        ILOAD + opCodeOffset + ilfdaOrd(staticType),
                        local
                    )
                }
            }
        }

        override fun toString(): String {
            return "RegistryAccess(op=$opCodeStr, key=$key, store=$store, wide=$wide)"
        }

        companion object {
            fun raw(local: Int, stype: Int, store: Boolean): RegistryAccess {
                val newRegAccess = RegistryAccess(0, stype, store)
                newRegAccess.calculateBytecode(local)
                return newRegAccess
            }
        }

        data class Key(
            @JvmField
            val registryId: Int,
            @JvmField
            val staticType: Int,
        ) {
            override fun toString(): String {
                return "Key(registryId=$registryId, staticType=${staticTypeToStr(staticType)})"
            }
        }
    }

    abstract class PrimitiveConstant(
        @JvmField
        val staticType: Int,
    ): JvmInstruction() {

        @JvmField
        val wide: Boolean = Scalars.isWide(staticType)

        override lateinit var bytecode: ByteArray

        internal fun fixWithPool(pool: ConstantPool) {
            val b = bytecode
            if (b.size > 2) {
                fromPool(pool)?.let {
                    bytecode = it
                }
            }
        }

        internal abstract fun getConstantPoolData(): PoolData

        @Throws(ClassFileLimitExceeded::class)
        internal fun fromPool(pool: ConstantPool): ByteArray? {
            val index = pool.dataRef(getConstantPoolData())

            return if (index >= 0) {
                when {
                    wide -> byteArrayOf_u8u16(
                        LDC2_W,
                        index,
                    )
                    index >= 256 -> byteArrayOf_u8u16(
                        LDC_W,
                        index,
                    )
                    else -> byteArrayOf_u8u8(
                        LDC,
                        index,
                    )
                }
            } else {
                null
            }
        }
    }

    class Const32 internal constructor(
        staticType: Int,
        value: Int,
        pool: ConstantPool?,
        calculator: Calculator,
    ) : PrimitiveConstant(staticType) {

        @JvmField
        val value: Int = Calculator.normalize(staticType, value)

        init {
            val value = this.value
            bytecode = if (pool != null) {
                calculator.lookupOnly32(staticType, value) ?: fromPool(pool)
                    ?: throw ClassFileLimitExceeded()
            } else {
                calculator.calc32(staticType, value)
            }
        }

        override fun getConstantPoolData(): PoolData {
            return getPoolKey(staticType, value)
        }

        internal fun getPoolKey(staticType: Int, value: Int): PoolData {
            val tag = when (staticType) {
                Scalars.INT -> ConstantPool.CONSTANT_Integer
                Scalars.FLOAT -> ConstantPool.CONSTANT_Float
                else -> error("Invalid type: $staticType")
            }

            return PoolData.IntData(tag, value)
        }

        override fun toString(): String {
            return "Const32(op=$opCodeStr, staticType=${staticTypeToStr(staticType)}, value=$value, wide=$wide)"
        }
    }

    class Const64 internal constructor(
        staticType: Int,
        value: Long,
        pool: ConstantPool?,
        calculator: Calculator,
    ) : PrimitiveConstant(staticType) {

        @JvmField
        val value: Long = Calculator.normalize(staticType, value)

        init {
            val value = this.value
            bytecode = if (pool != null) {
                calculator.lookupOnly64(staticType, value) ?: fromPool(pool)
                ?: throw ClassFileLimitExceeded()
            } else {
                calculator.calc64(staticType, value)
            }
        }

        override fun getConstantPoolData(): PoolData {
            return getPoolKey(staticType, value)
        }

        internal fun getPoolKey(staticType: Int, value: Long): PoolData {
            val tag = when (staticType) {
                Scalars.DOUBLE -> ConstantPool.CONSTANT_Double
                Scalars.LONG -> ConstantPool.CONSTANT_Long
                else -> throw IllegalArgumentException("Invalid type")
            }

            return PoolData.LongData(tag, value)
        }

        override fun toString(): String {
            return "Const64(op=$opCodeStr, staticType=${staticTypeToStr(staticType)}, value=$value, wide=$wide)"
        }
    }

    class OtherConstant(
        override val bytecode: ByteArray
    ) : JvmInstruction() {

        val wide: Boolean // will be null, string or class - always single
            get() = false
    }

    open class LazyJumpBase(
        protected val target: Int,
        override var min: Int,
        override var max: Int,
    ) : JvmInstruction() {

        private val targets: IntArray = intArrayOf(target)

        override fun targets(): IntArray = targets

        fun widenIfNecessary(labels: IntObjectMap<Label>, positionMap: ObjectIntMap<JvmInstruction>): Boolean {
            val offset = positionMap[labels[target]!!] - positionMap[this]
            return if (offset < -32768 || offset >= 32768) {
                min = max
                true
            } else {
                false
            }
        }
    }

    class Goto(
        target: Int,
    ) : LazyJumpBase(
        target = target,
        min = 3,
        max = 5, // upper limit on length of bytecode
    ) {

        override var bytecode: ByteArray? = null

        override fun fallsThrough(): Boolean {
            return false
        }

        override fun calculateBytecode(positionMap: ObjectIntMap<JvmInstruction>, labels: IntObjectMap<out JvmInstruction>) {
            val offset = positionMap[labels[target]!!] - positionMap[this]
            bytecode = if (max == 3) {
                byteArrayOf_u8u16(
                    GOTO,
                    offset,
                )
            } else {
                byteArrayOf_u8u32(
                    GOTO_W,
                    offset,
                )
            }
        }

        override fun toString(): String {
            return "Goto(op=$opCodeStr, target=$target, min=$min, max=$max)"
        }
    }

    class If(
        private val op: Int,
        target: Int
    ) : LazyJumpBase(
        target = target,
        min = 3,
        max = 8 // upper limit on length of bytecode
    ) {

        override var bytecode: ByteArray? = null

        override fun calculateBytecode(positionMap: ObjectIntMap<JvmInstruction>, labels: IntObjectMap<out JvmInstruction>) {
            val offset = positionMap[labels[target]!!] - positionMap[this]

            bytecode = if (max == 3) {
                byteArrayOf_u8u16(
                    op,
                    offset,
                )
            } else {
                byteArrayOf_u8u16u8u32(
                    ifOpposite[op],
                    8,
                    GOTO_W,
                    offset - 3,
                )
            }
        }

        override fun toString(): String {
            return "If(op=$opCodeStr, target=$target, min=$min, max=$max)"
        }

        companion object {
            private val ifOpposite: IntIntMap = MutableIntIntMap(16).apply {
                putOpposite(IFEQ, IFNE)
                putOpposite(IFLT, IFGE)
                putOpposite(IFGT, IFLE)
                putOpposite(IF_ICMPEQ, IF_ICMPNE)
                putOpposite(IF_ICMPLT, IF_ICMPGE)
                putOpposite(IF_ICMPGT, IF_ICMPLE)
                putOpposite(IFNULL, IFNONNULL)
                putOpposite(IF_ACMPEQ, IF_ACMPNE)
            }

            internal fun MutableIntIntMap.putOpposite(index: Int, value: Int) {
                put(index, value)
                put(value, index)
            }
        }
    }

    class Switch(
        private val defaultTarget: Int,
        private val jumps: IntIntMap,
    ) : JvmInstruction() {
        private val isTable: Boolean
        val noPadSize: Int

        private val low: Int
        private val high: Int

        override var bytecode: ByteArray? = null
        override val min: Int = 0
        override val max: Int

        private val targets: IntArray = run {
            val targets = MutableIntSet()
            jumps.forEachValue {
                targets.add(it)
            }
            targets.add(defaultTarget)
            val sortedTargets = targets.toIntArray()
            sortedTargets.sort()
            sortedTargets
        }

        init {
            assert(!jumps.isEmpty())
            val jumpKeys = jumps.keyArray()
            low = jumpKeys.min()
            high = jumpKeys.max()

            val tableCount = high.toLong() - low.toLong() + 1
            val tableSize = 4 * (tableCount + 1)
            val jumpSize = 8L * jumps.size

            this.isTable = jumpSize > tableSize
            this.noPadSize = 9 + (if (this.isTable) tableSize else jumpSize).toInt()
            this.max = this.noPadSize.toInt() + 3
        }

        override fun fallsThrough(): Boolean {
            return false
        }

        override fun targets(): IntArray = targets

        override fun calculateBytecode(positionMap: ObjectIntMap<JvmInstruction>, labels: IntObjectMap<out JvmInstruction>) {
            val pos: Int = positionMap[this]
            val offset = positionMap[labels[defaultTarget]!!] - pos
            val pad = calculateSwitchPadding(pos)

            bytecode = withWriter { writer ->
                if (isTable) {
                    writer.u8(TABLESWITCH)

                    @Suppress("unused")
                    for (i in 0 until pad) {
                        writer.u8(0)
                    }

                    writer.u32(offset)
                    writer.u32(low)
                    writer.u32(high)

                    for (k in low until high + 1) {
                        val target: Int = jumps.getOrDefault(k, defaultTarget)
                        writer.u32(positionMap[labels[target]!!] - pos)
                    }
                } else {
                    writer.u8(LOOKUPSWITCH)

                    @Suppress("unused")
                    for (i in 0 until pad) {
                        writer.u8(0)
                    }

                    writer.u32(offset)
                    writer.u32(jumps.size)

                    val sortedKeys = jumps.keyArray()
                    sortedKeys.sort()
                    for (key in sortedKeys) {
                        val target: Int = jumps[key]
                        val offset = positionMap[labels[target]!!] - pos
                        writer.u32(key)
                        writer.u32(offset)
                    }
                }
            }
        }

        override fun toString(): String {
            return buildString {
                append("Switch(op=")
                append(opCodeStr)
                append(", defaultTarget=")
                append(defaultTarget)
                append(", jumps=")
                append(jumps)
                append(", isTable=")
                append(isTable)
                append(", noPadSize=")
                append(noPadSize)
                append(", low=")
                append(low)
                append(", high=")
                append(high)
                append(", bytecode=")
                appendByteCodeStr(this)
                append(", min=")
                append(min)
                append(", max=")
                append(max)
                append(')')
            }
        }

        companion object {
            fun calculateSwitchPadding(pos: Int): Int {
                return (4 - ((pos + 1) % 4)) % 4
            }
        }
    }

    open class Other(
        override val bytecode: ByteArray
    ) : JvmInstruction() {

        override fun fallsThrough(): Boolean {
            return !hasReturnOrThrowBytecode(bytecode)
        }

        override fun toString(): String {
            return buildString {
                append(this@Other::class.simpleName)
                append("(op=")
                append(opCodeStr)
                append(", bytecode=")
                appendByteCodeStr(this)
                append(')')
            }
        }

        private fun hasReturnOrThrowBytecode(bytecode: ByteArray): Boolean {
            if (bytecode.size != 1) return false
            val b = bytecode[0].toUByte().toInt()
            return when (b) {
                in IRETURN..RETURN -> true
                ATHROW -> true
                else -> false
            }
        }
    }

    class Pop(): Other(POP_BYTES)

    class Pop2(): Other(POP2_BYTES)

    class Dup(): Other(DUP_BYTES)

    class Dup2(): Other(DUP2_BYTES)

    class InvokeInterface(
        op: Int,
        methodRef: Int,
        descriptors: Array<ByteArray?>
    ): Other(byteArrayOf_u8u16u8u8(op, methodRef, descriptors.size, 0))

    class Invoke(op: Int, methodRef: Int): Other(byteArrayOf_u8u16(op, methodRef))
}
