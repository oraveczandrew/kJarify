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

import androidx.collection.ArrayMap
import androidx.collection.MutableObjectIntMap
import hu.oandras.kJarify.CODE
import hu.oandras.kJarify.byteArrayOf_u16u16u16u16
import hu.oandras.kJarify.dex.Method
import hu.oandras.kJarify.forEachElement
import hu.oandras.kJarify.jvm.JvmInstruction.*
import hu.oandras.kJarify.jvm.optimization.Consts
import hu.oandras.kJarify.jvm.optimization.JumpOptimization
import hu.oandras.kJarify.jvm.optimization.Jumps.calcMinimumPositions
import hu.oandras.kJarify.jvm.optimization.OptimizationOptions
import hu.oandras.kJarify.streams.withWriter
import hu.oandras.kJarify.sumBy

internal object BytecodeAssembler {

    @JvmStatic
    fun finishCodeAttrs(
        pool: ConstantPool,
        codeIRs: List<IRWriter>,
        opts: OptimizationOptions
    ): Map<Method, ByteArray> {
        // if we have any code, make sure to reserve pool slot for attr name
        if (!codeIRs.isEmpty()) {
            pool.utf8Ref(CODE)
        }

        if (opts.delayConsts) {
            val longIRs: List<IRWriter> = codeIRs.filter {
                it.calcUpperBound() >= 65536
            }

            // Now allocate constants used by potentially long methods
            if (!longIRs.isEmpty()) {
                Consts.allocateRequiredConstants(pool, longIRs)
            }

            // If there's space left in the constant pool, allocate constants used by short methods
            codeIRs.forEachElement { ir ->
                ir.flatInstructions!!.forEachElement { ins ->
                    if (ins is PrimitiveConstant) {
                        ins.fixWithPool(pool)
                    }
                }
            }
        }

        val map = ArrayMap<Method, ByteArray>(codeIRs.size)
        codeIRs.forEachElement { codeIR ->
            map.put(
                codeIR.method,
                writeCodeAttributeTail(codeIR, opts)
            )
        }
        return map
    }

    @JvmStatic
    private fun writeCodeAttributeTail(irdata: IRWriter, opts: OptimizationOptions): ByteArray {
        JumpOptimization.optimize(irdata)
        val byteCodeResult = createBytecode(irdata, opts)
        val bytecodeChunks = byteCodeResult.bytecodeChunks
        val excepts = byteCodeResult.packedExcepts

        return withWriter { stream ->
            stream.u16(300) // stack
            stream.u16(irdata.registryCount) // locals

            stream.u32(bytecodeChunks.sumOf { it.size })
            bytecodeChunks.forEachElement(stream::write)

            // exceptions
            stream.u16(excepts.size)
            excepts.forEachElement(stream::write)

            // attributes
            stream.u16(0)
        }
    }

    @JvmStatic
    @Throws(ClassFileLimitExceeded::class)
    private fun createBytecode(irdata: IRWriter, opts: OptimizationOptions): ByteCodeResult {
        val instructionsList = irdata.flatInstructions!!
        val positionDictionary = MutableObjectIntMap<JvmInstruction>(instructionsList.size)
        val endPos = calcMinimumPositions(instructionsList, positionDictionary)
        val bytecodeChunks = ArrayList<ByteArray>(instructionsList.size)

        instructionsList.forEachElement { ins ->
            if (ins is LazyJumpBase || ins is Switch) {
                ins.calculateBytecode(positionDictionary, irdata.labels)
            }

            val bytecode = ins.bytecode
            if (bytecode != null) {
                bytecodeChunks.add(bytecode)
            }
        }

        val bytecodeChunksSize = bytecodeChunks.sumBy { it.size }

        assert(bytecodeChunksSize == endPos)

        if (bytecodeChunksSize > 65535) {
            if (opts != OptimizationOptions.ALL) {
                throw ClassFileLimitExceeded()
            }
        }

        val exceptions = irdata.exceptions
        val exceptionsSize = exceptions.size
        val packedExcepts = if (exceptionsSize > 0) {
            val prevInstrMap = ArrayMap<JvmInstruction, JvmInstruction>(instructionsList.size / 2)
            for (i in 1 until instructionsList.size) {
                val instruction = instructionsList[i]
                if (instruction is Label) {
                    prevInstrMap.put(instruction, instructionsList[i - 1])
                }
            }

            val packedExcepts = ArrayList<ByteArray>(exceptionsSize)
            for (i in 0 until exceptionsSize) {
                val ex = exceptions[i]
                val s = prevInstrMap[ex.start]!!
                val sOffset: Int = positionDictionary[s]
                val eOffset: Int = positionDictionary[ex.end]
                val hOffset: Int = positionDictionary[ex.target]
                assert(sOffset <= eOffset)
                if (sOffset < eOffset) {
                    packedExcepts.add(
                        byteArrayOf_u16u16u16u16(
                            sOffset,
                            eOffset,
                            hOffset,
                            ex.javaClassType,
                        )
                    )
                } else {
                    println("Skipping zero width exception!")
                    assert(false)
                }
            }
            packedExcepts
        } else {
            emptyList()
        }

        return ByteCodeResult(bytecodeChunks, packedExcepts)
    }

    class ByteCodeResult(
        @JvmField
        val bytecodeChunks: List<ByteArray>,
        @JvmField
        val packedExcepts: List<ByteArray>,
    )
}

