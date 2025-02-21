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

package hu.oandras.kJarify.typeinference

import androidx.collection.*
import hu.oandras.kJarify.JAVA_LANG_THROWABLE
import hu.oandras.kJarify.dex.*
import hu.oandras.kJarify.forEachElement
import hu.oandras.kJarify.jvm.ArrayTypes
import hu.oandras.kJarify.jvm.JvmOps
import hu.oandras.kJarify.jvm.MathOps
import hu.oandras.kJarify.jvm.Scalars
import hu.oandras.kJarify.treeList.BooleanTreeList
import hu.oandras.kJarify.treeList.IntTreeList
import hu.oandras.kJarify.treeList.TreeList

@Suppress("unused")
internal object TypeInference {

    @JvmStatic
    private fun mergeTypes(old: TypeInfo, newTypeInfo: TypeInfo): TypeInfo {
        val temp = old.copy()
        temp.prims.merge(newTypeInfo.prims)
        temp.arrays.merge(newTypeInfo.arrays)
        temp.tainted.merge(newTypeInfo.tainted)
        return if (old.isSame(temp)) old else temp
    }

    @JvmStatic
    private fun fromParams(method: Method, numRegs: Int): TypeInfo {
        val isStatic = (method.access and Flags.ACC_STATIC) != 0
        val fullParameterTypes: Array<ByteArray?> = method.id.getSpacedParameterTypes(isStatic)
        val offset = numRegs - fullParameterTypes.size

        val prims = IntTreeList(defaultValue = Scalars.INVALID, func = Int::and)
        val arrays = TreeList(defaultValue = ArrayTypes.INVALID, func = ArrayTypes::merge)
        val tainted = BooleanTreeList(defaultValue = false, func = Boolean::or)

        for (i in fullParameterTypes.indices) {
            val desc = fullParameterTypes[i]
            if (desc != null) {
                prims[offset + i] = Scalars.fromDesc(desc)
                arrays[offset + i] = ArrayTypes.fromDesc(desc)
            }
        }
        return TypeInfo(prims, arrays, tainted)
    }

    @JvmStatic
    private val MATH_THROW_OPS: IntArray = intArrayOf(
        JvmOps.IDIV,
        JvmOps.IREM,
        JvmOps.LDIV,
        JvmOps.LREM
    )

    @JvmStatic
    private fun pruneHandlers(allHandlers: ArrayMap<DalvikInstruction, out List<CatchItem>>): Map<DalvikInstruction, List<CatchItem>> {
        val result: ArrayMap<DalvikInstruction, MutableList<CatchItem>> = ArrayMap()

        for (i in 0 until allHandlers.size) {
            val instr = allHandlers.keyAt(i)
            val dalvikOpcode = instr.type

            if (!PRUNED_THROW_TYPES.contains(dalvikOpcode)) {
                continue
            }

            if (dalvikOpcode == DalvikOpcode.BinaryOp) {
                if (!MATH_THROW_OPS.contains(MathOps.BINARY[instr.opcode]!![0])) {
                    continue
                }
            } else if (dalvikOpcode == DalvikOpcode.BinaryOpConst) {
                if (!MATH_THROW_OPS.contains(MathOps.BINARY_LIT[instr.opcode])) {
                    continue
                }
            }

            val handlers = allHandlers.valueAt(i)

            val types: ArraySet<ByteArray> = ArraySet()
            for (k in handlers.indices) {
                val handler = handlers[k]
                val className: ByteArray = handler.className
                if (!types.contains(className)) {
                    result.getOrPut(instr) {
                        ArrayList()
                    }.add(handler)
                    types.add(className)
                }

                if (className.contentEquals(JAVA_LANG_THROWABLE)) {
                    break
                }
            }
        }

        return result
    }

    @JvmStatic
    private val CONTROL_FLOW_OPS: Set<DalvikOpcode> = arraySetOf(
        DalvikOpcode.Goto,
        DalvikOpcode.If,
        DalvikOpcode.IfZ,
        DalvikOpcode.Switch
    )

    @JvmStatic
    private fun collectHandlers(
        code: CodeItem,
    ): Map<DalvikInstruction, List<CatchItem>> {
        if (code.tries.isEmpty()) {
            return emptyMap()
        }

        // get exception handlers
        val allHandlers: ArrayMap<DalvikInstruction, ArrayList<CatchItem>> = ArrayMap()
        for (tryItem in code.tries) {
            for (instr in code.byteCode) {
                if (tryItem.start < instr.pos2 && tryItem.end > instr.position) {
                    allHandlers.getOrPut(instr) {
                        ArrayList()
                    }.addAll(tryItem.catches)
                }
            }
        }

        return if (allHandlers.isNotEmpty()) {
            pruneHandlers(allHandlers)
        } else {
            allHandlers
        }
    }

    class InferenceResult(
        @JvmField
        val types: IntObjectMap<TypeInfo>,
        @JvmField
        val allHandlers: Map<DalvikInstruction, List<CatchItem>>,
    )

    @Suppress("LocalVariableName")
    @JvmStatic
    fun doInference(
        dex: DexFile,
        method: Method,
        code: CodeItem,
        byteCode: Array<DalvikInstruction>,
        instructionDictionary: IntObjectMap<DalvikInstruction>
    ): InferenceResult {
        val allHandlers = collectHandlers(code)

        val types: MutableIntObjectMap<TypeInfo> = MutableIntObjectMap(byteCode.size)
        types.put(0, fromParams(method, code.registerCount))
        val dirty = MutableIntSet()
        dirty.add(0)

        val FUNCS = DalvikInstructionVisitor.FUNCS

        while (!dirty.isEmpty()) { // iterate until convergence
            for (instr in byteCode) {
                if (!dirty.contains(instr.position)) {
                    continue
                }

                dirty.remove(instr.position)
                val cur: TypeInfo = types[instr.position]!!
                val instructionType = instr.type
                var after: TypeInfo
                val funcIndex = FUNCS.indexOfKey(instructionType)
                if (funcIndex >= 0) {
                    after = FUNCS.valueAt(funcIndex)
                        .invoke(
                            dex = dex,
                            instr = instr,
                            cur = cur
                        )
                } else if (CONTROL_FLOW_OPS.contains(instructionType)) {
                    // control flow - none of these are in FUNCS
                    var after2 = cur
                    after = after2
                    var result = after

                    val implicitCasts = instr.implicitCasts
                    if (implicitCasts != null) {
                        val descInd = implicitCasts.descriptorIndex
                        val regs = implicitCasts.registries
                        regs.forEach { reg ->
                            val st: Int = cur.prims[reg] // could != OBJ if null
                            val at: ByteArray = ArrayTypes.narrow(
                                t1 = cur.arrays[reg],
                                t2 = ArrayTypes.fromDesc(dex.type(descInd))
                            )
                            result = result.assign(reg, st, at, true)
                        }
                        // merge into branch if op = if-nez else merge into fallthrough
                        if (instr.opcode == 0x39) {
                            after2 = result
                        } else {
                            after = result
                        }
                    }

                    when (instr.type) {
                        DalvikOpcode.Goto -> {
                            doMerge(instr.getIntArg(0), after2, instructionDictionary, types, dirty)
                        }

                        DalvikOpcode.If -> {
                            doMerge(instr.getIntArg(2), after2, instructionDictionary, types, dirty)
                        }

                        DalvikOpcode.IfZ -> {
                            doMerge(instr.getIntArg(1), after2, instructionDictionary, types, dirty)
                        }

                        DalvikOpcode.Switch -> {
                            val switchData = instructionDictionary[instr.getIntArg(1)]!!.switchData!!
                            switchData.forEachValue { offset ->
                                val target: Int = instr.position + offset
                                doMerge(target, cur, instructionDictionary, types, dirty)
                            }
                        }

                        else -> {
                            // do nothing
                        }
                    }
                } else {
                    after = cur
                }

                // these instructions don't fallthrough
                when (instr.type) {
                    DalvikOpcode.Return,
                    DalvikOpcode.Throw,
                    DalvikOpcode.Goto -> {
                        // do nothing
                    }
                    else -> {
                        doMerge(instr.pos2, after, instructionDictionary, types, dirty)
                    }
                }

                // exception handlers
                allHandlers[instr]?.forEachElement { handler ->
                    doMerge(handler.pos, cur, instructionDictionary, types, dirty)
                }
            }
        }

        return InferenceResult(types, allHandlers)
    }

    @JvmStatic
    private fun doMerge(
        pos: Int,
        newObj: TypeInfo,
        instructionDictionary: IntObjectMap<DalvikInstruction>,
        types: MutableIntObjectMap<TypeInfo>,
        dirty: MutableIntSet,
    ) {
        // prevent infinite loops
        if (!instructionDictionary.containsKey(pos)) {
            return
        }

        if (types.containsKey(pos)) {
            val old: TypeInfo = types[pos]!!
            val merged: TypeInfo = mergeTypes(old, newObj)
            if (merged !== old) {
                types.put(pos, merged)
                dirty.add(pos)
            }
        } else {
            types.put(pos, newObj)
            dirty.add(pos)
        }
    }
}
