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

import androidx.collection.ArraySet
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableObjectIntMap
import hu.oandras.kJarify.JAVA_LANG_THROWABLE
import hu.oandras.kJarify.dex.DalvikInstruction
import hu.oandras.kJarify.dex.DalvikOpcode.MoveResult
import hu.oandras.kJarify.dex.Flags
import hu.oandras.kJarify.dex.Method
import hu.oandras.kJarify.forEachElement
import hu.oandras.kJarify.jvm.DalvikInstructionVisitor.Companion.VISIT_FUNCS
import hu.oandras.kJarify.jvm.JvmInstruction.*
import hu.oandras.kJarify.jvm.Scalars.paramTypes
import hu.oandras.kJarify.jvm.constants.Calculator
import hu.oandras.kJarify.jvm.optimization.OptimizationOptions
import hu.oandras.kJarify.keyArray
import hu.oandras.kJarify.typeinference.TypeInference
import hu.oandras.kJarify.typeinference.TypeInfo
import java.util.*

class IRWriter private constructor(
    private val params: Parameters,
    @JvmField
    val method: Method,
    private val types: IntObjectMap<TypeInfo>,
    blockCountHint: Int,
) {

    internal class Parameters(
        override val pool: ConstantPool,
        override val calculator: Calculator,
        override val optimizationOptions: OptimizationOptions,
    ): IRBlock.ConstParameters

    private val _irBlocks: MutableIntObjectMap<IRBlock> = MutableIntObjectMap(blockCountHint)

    val irBlocks: IntObjectMap<IRBlock>
        get() = _irBlocks

    @JvmField
    var flatInstructions: MutableList<JvmInstruction>? = null

    private var _exceptions: MutableList<ExceptionData>? = null

    val exceptions: List<ExceptionData>
        get() = _exceptions ?: emptyList()

    private val _labels: MutableIntObjectMap<Label> = MutableIntObjectMap(1)

    val labels: IntObjectMap<Label>
        get() = _labels

    @JvmField
    var initialArgs: MutableList<RegistryAccess.Key?>? = null

    private var exceptionRedirects: MutableIntObjectMap<Label>? = null

    private var _exceptionStarts: MutableSet<Label>? = null

    val exceptionStarts: Set<Label>?
        get() = _exceptionStarts

    private var _exceptionEnds: MutableSet<Label>? = null

    val exceptionEnds: Set<Label>?
        get() = _exceptionEnds

    @JvmField
    val jumpTargets: MutableSet<JvmInstruction> = HashSet()

    @JvmField
    val targetPredCounts: MutableObjectIntMap<JvmInstruction> = MutableObjectIntMap()

    @JvmField
    var registryCount: Int = 0 // will be set once registers are allocated

    fun calcInitialArgs(nRegs: Int, scalarPtypes: IntArray) {
        val scalarTypesSize = scalarPtypes.size
        initialArgs = ArrayList<RegistryAccess.Key?>(scalarTypesSize).also {
            val regOff = nRegs - scalarTypesSize
            for (i in 0 until scalarTypesSize) {
                val st: Int = scalarPtypes[i]
                if (st == Scalars.INVALID) {
                    it.add(null)
                } else {
                    it.add(RegistryAccess.Key(i + regOff, st))
                }
            }
        }
    }

    fun addException(exception: ExceptionData) {
        val exceptions = _exceptions ?: ArrayList<ExceptionData>().also {
            _exceptions = it
        }
        exceptions.add(exception)
    }

    fun addExceptionRedirect(target: Int): Label {
        val exceptionRedirects = exceptionRedirects ?: MutableIntObjectMap<Label>().also {
            exceptionRedirects = it
        }

        return exceptionRedirects.getOrPut(target) { Label() }
    }

    fun addExceptionStart(label: Label) {
        val set = _exceptionStarts ?: ArraySet<Label>().also {
            _exceptionStarts = it
        }
        set.add(label)
    }

    fun isExceptionStart(instruction: JvmInstruction): Boolean {
        return _exceptionStarts?.contains(instruction) == true
    }

    fun addExceptionEnd(label: Label) {
        val set = _exceptionEnds ?: ArraySet<Label>().also {
            _exceptionEnds = it
        }
        set.add(label)
    }

    fun isExceptionEnd(instruction: JvmInstruction): Boolean {
        return _exceptionEnds?.contains(instruction) == true
    }

    fun createBlock(instr: DalvikInstruction): IRBlock {
        val position = instr.position
        val block = IRBlock(
            params = params,
            pos = position,
            typeData = types[position]!!,
        )
        _irBlocks.put(block.pos, block)
        _labels.put(block.pos, block.instructions[0] as Label)
        return block
    }

    fun flatten() {
        val irBlocks = _irBlocks
        val exceptionRedirects = exceptionRedirects

        var flattenSize = 0
        irBlocks.forEachValue {
            flattenSize += it.instructions.size
        }

        val instructions = ArrayList<JvmInstruction>(flattenSize)
        val sortedKeys = irBlocks.keyArray()
        sortedKeys.sort()
        for (pos in sortedKeys) {
            if (exceptionRedirects != null && exceptionRedirects.containsKey(pos)) {
                if (!instructions.isEmpty() && !(instructions[instructions.lastIndex]).fallsThrough()) {
                    instructions.add(exceptionRedirects.remove(pos)!!)
                    instructions.add(Pop())
                }
            }
            instructions.addAll(irBlocks[pos]!!.instructions)
        }

        if (exceptionRedirects != null) {
            for (target in sortedKeys) {
                val label = exceptionRedirects[target]
                if (label != null) {
                    instructions.add(label)
                    instructions.add(Pop())
                    instructions.add(Goto(target))
                }
            }
        }

        flatInstructions = instructions
        irBlocks.clear()
        this.exceptionRedirects = null
    }

    fun replaceInstructions(replace: Map<in JvmInstruction, List<JvmInstruction>>) {
        if (replace.isEmpty()) {
            replace
        }

        val instructions = flatInstructions!!

        for (i in instructions.indices.reversed()) {
            val instr = instructions[i]
            val replacement = replace[instr]
            if (replacement != null) {
                instructions.removeAt(i)
                if (replacement.isNotEmpty()) {
                    instructions.addAll(i, replacement)
                }
            }
        }

        //assert(instructions.toSet().size == instructions.size)
    }

    fun calcUpperBound(): Int {
        return flatInstructions!!.sumOf { ins ->
            ins.bytecode?.size ?: ins.max
        }
    }

    fun isJumpTarget(instruction: JvmInstruction): Boolean {
        return jumpTargets.contains(instruction)
    }

    override fun toString(): String {
        return "IRWriter(method=$method, registryCount: $registryCount, instructionCount: ${flatInstructions?.size ?: 0})"
    }

    companion object {

        internal fun writeBytecode(params: Parameters, method: Method): IRWriter {
            val dex = method.dex
            val code = method.code!!
            val byteCode = code.byteCode

            val instructionDictionary: MutableIntObjectMap<DalvikInstruction> = MutableIntObjectMap(byteCode.size)

            for (instr in byteCode) {
                instructionDictionary.put(instr.position, instr)
            }

            val inferenceResult = TypeInference.doInference(dex, method, code, byteCode, instructionDictionary)
            val types = inferenceResult.types
            val allHandlers = inferenceResult.allHandlers

            val scalarPtypes = paramTypes(method.id, (method.access and Flags.ACC_STATIC) != 0)

            val reachableInstructions = byteCode.filter {
                types.containsKey(it.position)
            }

            val writer = IRWriter(
                params = params,
                method = method,
                types = types,
                blockCountHint = reachableInstructions.size,
            )

            writer.calcInitialArgs(code.registerCount, scalarPtypes)

            reachableInstructions.forEachElement { instr ->
                VISIT_FUNCS[instr.type]!!.invoke(
                    method = method,
                    dex = dex,
                    instructionDictionary = instructionDictionary,
                    typeData = types[instr.position]!!,
                    block = writer.createBlock(instr),
                    instr = instr
                )
            }

            val pool = params.pool
            val labels = writer.labels

            if (allHandlers.isNotEmpty()) {
                val irBlocks = writer.irBlocks
                val sortedHandlers: List<DalvikInstruction> = allHandlers.keys.sortedBy {
                    it.position
                }

                sortedHandlers.forEachElement { instr ->
                    val instructionPos = instr.position
                    if (!types.containsKey(instructionPos)) { // skip unreachable instructions
                        return@forEachElement
                    }

                    val handlers = allHandlers[instr]!!

                    val exceptionLabels: Pair<Label, Label> = irBlocks[instructionPos]!!.generateExceptLabels()
                    val start: Label = exceptionLabels.first
                    val end: Label = exceptionLabels.second
                    writer.addExceptionStart(start)
                    writer.addExceptionEnd(end)

                    handlers.forEachElement { entry ->
                        val ctype: ByteArray = entry.className
                        val handlerPos: Int = entry.pos
                        // If handler doesn't use the caught exception, we need to redirect to a pop instead
                        val target: Label = if (instructionDictionary[handlerPos]!!.type != MoveResult) {
                            writer.addExceptionRedirect(handlerPos)
                        } else {
                            labels[handlerPos]!!
                        }
                        writer.jumpTargets.add(target)
                        writer.targetPredCounts.put(target, writer.targetPredCounts.getOrDefault(target, 0) + 1)

                        // When catching Throwable, we can use the special index 0 instead,
                        // potentially saving a constant pool entry or two
                        val javaClassType = if (ctype.contentEquals(JAVA_LANG_THROWABLE)) 0 else pool.classRef(ctype)
                        writer.addException(ExceptionData(start, end, target, javaClassType))
                    }
                }
            }

            writer.flatten()

            // find jump targets (in addition to exception handler targets)
            writer.flatInstructions!!.forEachElement { instr ->
                for (target in instr.targets()) {
                    val label: Label = labels[target]!!
                    writer.jumpTargets.add(label)
                    writer.targetPredCounts.put(label, writer.targetPredCounts.getOrDefault(label, 0) + 1)
                }
            }

            return writer
        }
    }
}
