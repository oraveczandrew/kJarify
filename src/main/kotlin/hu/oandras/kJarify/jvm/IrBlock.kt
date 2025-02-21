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
import androidx.collection.MutableIntIntMap
import hu.oandras.kJarify.*
import hu.oandras.kJarify.dex.DexFile
import hu.oandras.kJarify.jvm.ArrayOpByteCodeLookUp.ByteArrayKey
import hu.oandras.kJarify.jvm.ArrayOpByteCodeLookUp.newArrayCodes
import hu.oandras.kJarify.jvm.JvmInstruction.*
import hu.oandras.kJarify.jvm.constants.Calculator
import hu.oandras.kJarify.jvm.optimization.Dup2izeOptimization
import hu.oandras.kJarify.jvm.optimization.OptimizationOptions
import hu.oandras.kJarify.typeinference.TypeInfo

class IRBlock internal constructor(
    @JvmField
    val pos: Int,
    @JvmField
    internal val typeData: TypeInfo,
    params: ConstParameters,
) {

    @JvmField
    internal val pool: ConstantPool = params.pool

    private val delayConsts: Boolean = params.optimizationOptions.delayConsts

    private val calculator: Calculator = params.calculator

    internal interface ConstParameters {
        val pool: ConstantPool
        val calculator: Calculator
        val optimizationOptions: OptimizationOptions
    }

    private val _instructions: ArrayList<JvmInstruction> = ArrayList<JvmInstruction>(5).apply {
        add(Label(pos))
    }

    val instructions: List<JvmInstruction>
        get() = _instructions

    fun add(jvmInstr: JvmInstruction) {
        _instructions.add(jvmInstr)
    }

    @PublishedApi
    internal fun addOther(bytecode: ByteArray) {
        add(Other(bytecode))
    }

    fun ldc(index: Int) {
        val bytecode = if (index < 256) {
            byteArrayOf_u8u8(JvmOps.LDC, index)
        } else {
            byteArrayOf_u8u16(JvmOps.LDC_W, index)
        }

        add(OtherConstant(bytecode))
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun load(reg: Int, staticType: Int, desc: ByteArray? = null, clsName: ByteArray? = null) {
        loadImpl(reg, staticType, desc, clsName)
    }

    @PublishedApi
    internal fun loadImpl(reg: Int, staticType: Int, desc: ByteArray?, clsName: ByteArray?) {
        var className = clsName

        if (typeData.arrays[reg].contentEquals(ArrayTypes.NULL)) {
            const32(0, staticType)
        } else {
            add(RegistryAccess(
                registryId = reg,
                staticType = staticType,
                store = false
            ))

            if (staticType == Scalars.OBJ && typeData.tainted[reg]) {
                assert(desc == null || className == null)

                if (className == null) {
                    className = if (desc != null && desc.startsWith('L')) {
                        desc.copyOfRange(1, desc.size - 1)
                    } else {
                        desc
                    }
                }

                if (className != null && !className.contentEquals(JAVA_LANG_OBJECT)) {
                    addOther(byteArrayOf_u8u16(JvmOps.CHECKCAST, pool.classRef(className)))
                }
            }
        }
    }

    fun loadAsArray(reg: Int) {
        val at: ByteArray = typeData.arrays[reg]
        if (at.contentEquals(ArrayTypes.NULL)) {
            constNull()
        } else {
            add(RegistryAccess(reg, Scalars.OBJ, false))

            if (typeData.tainted[reg]) {
                val className = if (at.contentEquals(ArrayTypes.INVALID)) {
                    ARRAY_OF_JAVA_LANG_OBJECT
                } else {
                    at
                }

                addOther(byteArrayOf_u8u16(JvmOps.CHECKCAST, pool.classRef(className)))
            }
        }
    }

    fun store(reg: Int, stype: Int) {
        add(RegistryAccess(reg, stype, true))
    }

    fun returnOp() {
        addOther(RETURN_BYTES)
    }

    fun returnOp(staticType: Int) {
        addOther(byteArrayOf_u8(JvmOps.IRETURN + ilfdaOrd(staticType)))
    }

    fun constNull() {
        add(OtherConstant(ACONST_NULL_BYTES))
    }

    internal inline fun fillArraySub(
        op: Int,
        dataCount: Int,
        pop: Boolean = true,
        callback: (index: Int) -> Unit
    ) {
        val opBytes = byteArrayOf_u8(op)

        val gen = Dup2izeOptimization.genDups(dataCount, if (pop) 0 else 1)
        for (i in 0 until dataCount) {
            gen.next().forEachElement(::add)
            const32(i, Scalars.INT)
            callback(i)
            addOther(opBytes)
        }
        // may need to pop at end
        gen.next().forEachElement(::add)
    }

    @Suppress("DEPRECATION")
    fun newArray(arrayTypeDesc: ByteArray) {
        val key = ByteArrayKey(arrayTypeDesc)

        val bytecode = if (newArrayCodes.containsKey(key)) {
            byteArrayOf_u8u8(JvmOps.NEWARRAY, newArrayCodes[key])
        } else {
            // can be either multidim array or object array descriptor
            val typeDesc = if (
                arrayTypeDesc[0] == '['.toInt().toByte() &&
                arrayTypeDesc[1] == 'L'.toInt().toByte()
            ) {
                arrayTypeDesc.copyOfRange(2, arrayTypeDesc.size - 1)
            } else {
                arrayTypeDesc.copyOfRange(1, arrayTypeDesc.size)
            }

            byteArrayOf_u8u16(JvmOps.ANEWARRAY, pool.classRef(typeDesc))
        }

        addOther(bytecode)
    }

    @Suppress("DuplicatedCode")
    fun fillArrayData(op: Int, staticType: Int, values: LongArray) {
        fillArraySub(op, values.size) { dataIndex ->
            const64(values[dataIndex], staticType)
        }
    }

    @Suppress("DuplicatedCode")
    fun fillArrayData(op: Int, staticType: Int, values: IntArray) {
        fillArraySub(op, values.size) { dataIndex ->
            const32(values[dataIndex], staticType)
        }
    }

    fun const64(value: Long, staticType: Int) {
        if (staticType == Scalars.OBJ) {
            assert(value == 0L)
            constNull()
        } else {
            assert(staticType == Scalars.LONG || staticType == Scalars.DOUBLE)

            // If constant pool is simple, assume we're in non-opt mode and only use
            // the constant pool for generating constants instead of calculating
            // bytecode sequences for them. If we're in opt mode, pass null for pool
            // to generate bytecode instead
            add(Const64(
                staticType = staticType,
                value = value,
                pool = if (delayConsts) null else pool,
                calculator = calculator,
            ))
        }
    }

    fun const32(value: Int, staticType: Int) {
        if (staticType == Scalars.OBJ) {
            assert(value == 0)
            constNull()
        } else {
            assert(staticType == Scalars.INT || staticType == Scalars.FLOAT)

            // If constant pool is simple, assume we're in non-opt mode and only use
            // the constant pool for generating constants instead of calculating
            // bytecode sequences for them. If we're in opt mode, pass null for pool
            // to generate bytecode instead
            add(Const32(
                staticType = staticType,
                value = value,
                pool = if (delayConsts) null else pool,
                calculator = calculator,
            ))
        }
    }

    fun cast(dex: DexFile, reg: Int, index: Int) {
        load(reg, Scalars.OBJ)
        addOther(
            byteArrayOf_u8u16(JvmOps.CHECKCAST, pool.classRef(dex.classType(index)))
        )
        store(reg, Scalars.OBJ)
    }

    fun goto(target: Int) {
        add(Goto(target))
    }

    fun ifCondition(op: Int, target: Int) {
        add(If(op, target))
    }

    fun switchCase(defaultLabel: Int, jumps: IntIntMap) {
        val filteredJumps = MutableIntIntMap(jumps.size)

        jumps.forEach { key, value ->
            if (value != defaultLabel) {
                filteredJumps.put(key, value)
            }
        }

        if (filteredJumps.isEmpty()) {
            add(Pop())
            goto(defaultLabel)
        } else {
            add(Switch(defaultLabel, filteredJumps))
        }
    }

    fun generateExceptLabels(): Pair<Label, Label> {
        val instructions = _instructions

        var sIndex = 0
        var endIndex = instructions.size
        // assume only Other instructions can throw
        while (sIndex < endIndex && instructions[sIndex] !is Other) {
            sIndex++
        }
        while (sIndex < endIndex && instructions[endIndex - 1] !is Other) {
            endIndex--
        }

        assert(sIndex < endIndex)
        val startLabel = Label()
        val endLabel = Label()
        instructions.add(sIndex, startLabel)
        instructions.add(endIndex + 1, endLabel)

        return Pair(startLabel, endLabel)
    }
}
