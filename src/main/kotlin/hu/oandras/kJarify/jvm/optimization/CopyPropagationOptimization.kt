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

internal object CopyPropagationOptimization : JvmOptimization() {

    override fun optimize(irData: IRWriter) {
        val instructionList: List<JvmInstruction> = irData.flatInstructions!!
        val targetPredCounts = irData.targetPredCounts
        val replace: ArrayMap<JvmInstruction, List<JvmInstruction>> = ArrayMap()

        val singlePredInfos: ArrayMap<JvmInstruction, CopySetsMap<JvmInstruction.RegistryAccess.Key>> = ArrayMap()

        var prev: JvmInstruction? = null
        var current = CopySetsMap<JvmInstruction.RegistryAccess.Key>()

        instructionList.forEachElement { instr ->
            // reset all info when control flow is merged
            if (irData.isJumpTarget(instr)) {
                // try to use info if this was a single predecessor forward jump
                current = if (prev != null && !prev.fallsThrough() && targetPredCounts[instr] == 1) {
                    singlePredInfos.getOrElse(instr) {
                        CopySetsMap()
                    }
                } else {
                    CopySetsMap()
                }
            } else if (instr is JvmInstruction.RegistryAccess) {
                val key: JvmInstruction.RegistryAccess = instr
                if (key.store) {
                    // check if previous instr was a load
                    if (prev is JvmInstruction.RegistryAccess && !prev.store) {
                        if (!current.move(key.key, prev.key)) {
                            replace[prev] = emptyList()
                            replace[instr] = emptyList()
                        }
                    } else {
                        current.clobber(key.key)
                    }
                } else {
                    val rootKey = current.load(key.key)
                    if (key.key != rootKey) {
                        assert(!replace.containsKey(instr))
                        // replace with load from root register instead
                        replace[instr] = listOf(
                            JvmInstruction.RegistryAccess(
                                registryId = rootKey.registryId,
                                staticType = rootKey.staticType,
                                store = false
                            )
                        )
                    }
                }
            } else {
                for (target in instr.targets()) {
                    val label: JvmInstruction.Label = irData.labels[target]!!
                    if (targetPredCounts[label] == 1) {
                        singlePredInfos[label] = current.copy()
                    }
                }
            }
            prev = instr
        }

        irData.replaceInstructions(replace)
    }
}