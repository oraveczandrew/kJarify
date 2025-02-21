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
import hu.oandras.kJarify.jvm.IRWriter
import hu.oandras.kJarify.jvm.JvmInstruction

internal object InlineConstsOptimization : JvmOptimization() {

    private class ConstInliner : NoExceptVisitorBase() {
        @JvmField
        val uses: ArrayMap<JvmInstruction.RegistryAccess, JvmInstruction.RegistryAccess> = ArrayMap()

        @JvmField
        val notMultiUsed: ArraySet<JvmInstruction.RegistryAccess> = ArraySet()

        @JvmField
        val current: ArrayMap<JvmInstruction.RegistryAccess.Key, JvmInstruction.RegistryAccess> = ArrayMap()

        override fun reset() {
            current.clear()
        }

        override fun visitReturn() {
            val notMultiUsed = notMultiUsed
            val current = current
            for (i in 0 until current.size) {
                notMultiUsed.add(current.valueAt(i))
            }
            reset()
        }

        override fun visit(instr: JvmInstruction) {
            if (instr is JvmInstruction.RegistryAccess) {
                val current = current
                val key = instr.key
                if (instr.store) {
                    val index = current.indexOfKey(key)
                    if (index >= 0) {
                        notMultiUsed.add(current.valueAt(index))
                    }
                    current.put(key, instr)
                } else {
                    val index = current.indexOfKey(key)
                    if (index >= 0) {
                        // if currently used 0, mark it used once
                        // if used once already, mark it as multi-used
                        val c = current.valueAt(index)

                        if (uses.containsKey(c)) {
                            current.remove(key)
                        } else {
                            uses.put(c, instr)
                        }
                    }
                }
            }
        }
    }

    override fun optimize(irdata: IRWriter) {
        val instructions: List<JvmInstruction> = irdata.flatInstructions!!
        val visitor = visitLinearCode(irdata, ConstInliner())

        val notMultiUsed = visitor.notMultiUsed

        if (notMultiUsed.isEmpty()) {
            return
        }

        val uses = visitor.uses

        val replace: ArrayMap<in JvmInstruction, ArrayList<JvmInstruction>> = ArrayMap(notMultiUsed.size)
        for (i in 0 until instructions.size - 1) {
            val ins1 = instructions[i]
            val ins2 = instructions[i + 1]

            if (notMultiUsed.contains(ins2) && (ins1 is JvmInstruction.PrimitiveConstant || ins1 is JvmInstruction.OtherConstant)) {
                replace.put(ins1, ArrayList())
                replace.put(ins2, ArrayList())
                if (uses.containsKey(ins2)) {
                    replace.getOrPut(uses[ins2]!!) {
                        ArrayList()
                    }.add(ins1)
                }
            }
        }

        irdata.replaceInstructions(replace)
    }
}