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
import androidx.collection.ArraySet
import hu.oandras.kJarify.forEachElement
import hu.oandras.kJarify.jvm.IRWriter
import hu.oandras.kJarify.jvm.JvmInstruction
import hu.oandras.kJarify.jvm.JvmInstruction.Pop
import hu.oandras.kJarify.jvm.JvmInstruction.Pop2

internal object UnusedRegisterRemovalOptimization: JvmOptimization() {

    override fun optimize(irData: IRWriter) {
        // Remove stores to registers that are not read from anywhere in the method
        val instructionList: List<JvmInstruction> = irData.flatInstructions!!

        val replace: ArrayMap<JvmInstruction, List<JvmInstruction>>? = findReplacements(instructionList)
        if (replace != null) {
            irData.replaceInstructions(replace)
        }
    }

    private fun findReplacements(
        instructionList: List<JvmInstruction>
    ): ArrayMap<JvmInstruction, List<JvmInstruction>>? {
        // Remove stores to registers that are not read from anywhere in the method
        val used: MutableSet<JvmInstruction.RegistryAccess.Key> = ArraySet()
        instructionList.forEachElement { instr ->
            if (instr is JvmInstruction.RegistryAccess && !instr.store) {
                used.add(instr.key)
            }
        }

        // Lazily allocated: most methods need no replacement at all
        var replace: ArrayMap<JvmInstruction, List<JvmInstruction>>? = null
        var prev: JvmInstruction? = null
        instructionList.forEachElement { instr ->
            if (instr is JvmInstruction.RegistryAccess && !used.contains(instr.key)) {
                assert(instr.store)
                // if prev instruction is load or const, just remove it and the store
                // otherwise, replace the store with a pop
                val map = replace ?: ArrayMap<JvmInstruction, List<JvmInstruction>>().also {
                    replace = it
                }
                if (prev != null && isRemovable(prev)) {
                    map[prev] = emptyList()
                    map[instr] = emptyList()
                } else {
                    map[instr] = listOf(if (instr.wide) Pop2() else Pop())
                }
            }
            prev = instr
        }

        return replace
    }

    private fun isRemovable(instr: JvmInstruction): Boolean {
        // can remove if load or const since we know there are no side effects
        // note - instr may be null
        if (instr is JvmInstruction.RegistryAccess && !instr.store) {
            return true
        }

        return instr is JvmInstruction.PrimitiveConstant || instr is JvmInstruction.OtherConstant
    }
}