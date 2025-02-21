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

internal object StoreLoadPruneOptimization: JvmOptimization() {

    private class StoreLoadPruner : NoExceptVisitorBase() {
        private val current: ArrayMap<JvmInstruction.RegistryAccess.Key, Pair<JvmInstruction.RegistryAccess, JvmInstruction.RegistryAccess>> =
            ArrayMap()
        private var last: JvmInstruction.RegistryAccess? = null // Replace Object with the actual type if known

        @JvmField
        val removed: ArraySet<JvmInstruction.RegistryAccess> =
            ArraySet() // Replace Object with the actual type if known

        override fun reset() {
            current.clear()
            last = null
        }

        override fun visitReturn() {
            for (i in 0 until current.size) {
                val pair = current.valueAt(i)
                assert(pair.first.store && !pair.second.store)
                removed.add(pair.first)
                removed.add(pair.second)
            }
            reset()
        }

        override fun visit(instr: JvmInstruction) {
            // Replace Object with the actual type if known
            if (instr is JvmInstruction.RegistryAccess) {
                val current = current
                val regAccess = instr
                val key = regAccess.key
                if (regAccess.store) {
                    val index = current.indexOfKey(key)
                    if (index >= 0) {
                        val pair = current.valueAt(index)
                        assert(pair.first.store && !pair.second.store)
                        removed.add(pair.first)
                        removed.add(pair.second)
                        current.removeAt(index)
                    }
                    last = instr
                } else {
                    current.remove(key)
                    val last = last
                    if (last != null && last.key == key) {
                        current.put(key, Pair(last, instr))
                    }
                    this.last = null
                }
            } else if (instr !is JvmInstruction.Label) {
                last = null
            }
        }
    }

    override fun optimize(irdata: IRWriter) {
        // Remove a store immediately followed by a load from the same register
        // (potentially with a label in between) if it can be proven that this
        // register isn't read again. As above, this only considers linear sections of code.
        // Must not be run before dup2ize!
        val data: StoreLoadPruner = visitLinearCode(irdata, StoreLoadPruner())

        if (data.removed.isEmpty()) {
            return
        }

        val replacements = HashMap<JvmInstruction, List<JvmInstruction>>()
        for (instr in data.removed) {
            replacements.put(instr, emptyList())
        }
        irdata.replaceInstructions(replacements)
    }
}