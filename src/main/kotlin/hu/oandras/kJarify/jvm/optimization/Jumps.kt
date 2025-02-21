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

@file:Suppress("SameParameterValue")

package hu.oandras.kJarify.jvm.optimization

import androidx.collection.MutableObjectIntMap
import hu.oandras.kJarify.forEachElement
import hu.oandras.kJarify.jvm.JvmInstruction
import hu.oandras.kJarify.jvm.JvmInstruction.LazyJumpBase
import hu.oandras.kJarify.jvm.JvmInstruction.Switch

internal object Jumps {

    @JvmStatic
    fun calcMinimumPositions(instructions: List<JvmInstruction>, outMap: MutableObjectIntMap<JvmInstruction>): Int {
        assert(outMap.isEmpty())

        var pos = 0

        instructions.forEachElement { ins ->
            outMap.put(ins, pos)

            when (ins) {
                is LazyJumpBase -> {
                    pos += ins.min
                }

                is Switch -> {
                    val pad = Switch.calculateSwitchPadding(pos)
                    pos += pad + ins.noPadSize
                }

                else -> {
                    pos += ins.bytecode?.size ?: 0
                }
            }
        }

        return pos
    }
}

