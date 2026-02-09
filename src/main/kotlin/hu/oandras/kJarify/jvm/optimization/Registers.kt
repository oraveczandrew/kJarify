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

package hu.oandras.kJarify.jvm.optimization

import androidx.collection.ArrayMap
import hu.oandras.kJarify.forEachElement
import hu.oandras.kJarify.jvm.IRWriter
import hu.oandras.kJarify.jvm.JvmInstruction
import hu.oandras.kJarify.jvm.JvmInstruction.RegistryAccess
import hu.oandras.kJarify.jvm.Scalars.isWide
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal object Registers {

    // Allocate registers to JVM registers on a first come, first served basis
    // For simplicity, parameter registers are preserved as is
    fun simpleAllocateRegisters(irData: IRWriter) {
        val instructionList: List<JvmInstruction> = irData.flatInstructions!!
        val registryMap: ArrayMap<RegistryAccess.Key?, Int> = ArrayMap()
        val initialArgs = irData.initialArgs!!

        for (i in initialArgs.indices) {
            registryMap[initialArgs[i]] = i
        }

        var nextRegistry = initialArgs.size

        instructionList.forEachElement { instr ->
            if (instr is RegistryAccess) {
                val regAccess: RegistryAccess = instr

                val regIndex = registryMap.getOrPut(regAccess.key) {
                    val toPut = nextRegistry
                    nextRegistry++
                    if (regAccess.wide) {
                        nextRegistry++
                    }
                    toPut
                }

                regAccess.calculateBytecode(regIndex)
            }
        }

        irData.registryCount = nextRegistry
    }

    fun sortAllocateRegisters(irData: IRWriter) {
        val initialArgs = irData.initialArgs!!
        val instructionList: MutableList<JvmInstruction> = irData.flatInstructions!!

        val useCounts: MutableMap<RegistryAccess.Key, Int> = HashMap()
        instructionList.forEachElement { instr ->
            if (instr is RegistryAccess) {
                val key = instr.key
                useCounts[key] = useCounts.getOrDefault(key, 0) + 1
            }
        }

        val regs = initialArgs.toMutableList()

        val rest: List<RegistryAccess.Key> = useCounts.keys.sortedWith { k1, k2 ->
            val countComparison = useCounts[k2]!!.compareTo(useCounts[k1]!!)
            if (countComparison != 0) {
                countComparison
            } else {
                k1.registryId.compareTo(k2.registryId)
            }
        }

        rest.forEachElement { key ->
            if (!initialArgs.contains(key)) {
                regs.add(key)
                if (isWide(key.staticType)) {
                    regs.add(null)
                }
            }
        }

        val candidateI = max(4, initialArgs.size)
        if (regs.size > candidateI && regs[candidateI] != null) {
            val candidate = regs[candidateI]!!
            if (!isWide(candidate.staticType) && useCounts[candidate]!! >= 3) {
                for (i in 0 until min(4, initialArgs.size)) {
                    if (regs[i] == null || regs[i + 1] == null) {
                        continue
                    }

                    val target = regs[i]!!
                    if (useCounts[candidate]!! > useCounts[target]!! + 3) {
                        Collections.swap(regs, i, candidateI)
                        val load = RegistryAccess.raw(i, target.staticType, false)
                        val store = RegistryAccess(target.registryId, target.staticType, true)
                        instructionList.add(0, load)
                        instructionList.add(1, store)
                        irData.flatInstructions = instructionList
                        break
                    }
                }
            }
        }

        irData.registryCount = regs.size
        val regMap: MutableMap<RegistryAccess.Key?, Int> = HashMap()
        for (i in regs.indices) {
            val reg = regs[i]
            if (reg != null) {
                regMap[reg] = i
            }
        }

        instructionList.forEachElement { instr ->
            if (instr.bytecode == null && instr is RegistryAccess) {
                instr.calculateBytecode(regMap[instr.key]!!)
            }
        }
    }
}
