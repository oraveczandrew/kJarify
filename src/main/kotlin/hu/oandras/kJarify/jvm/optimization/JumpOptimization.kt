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

import androidx.collection.MutableObjectIntMap
import hu.oandras.kJarify.forEachElement
import hu.oandras.kJarify.jvm.IRWriter
import hu.oandras.kJarify.jvm.JvmInstruction

internal object JumpOptimization: JvmOptimization() {

    override fun optimize(irdata: IRWriter) {
        val instructionList = irdata.flatInstructions!!
        val jumpInstructions = instructionList.filterIsInstance<JvmInstruction.LazyJumpBase>()

        if (jumpInstructions.isEmpty()) {
            return
        }

        val positionMap = MutableObjectIntMap<JvmInstruction>(instructionList.size)

        while (true) {
            var done = true

            positionMap.clear()
            Jumps.calcMinimumPositions(instructionList, positionMap)

            jumpInstructions.forEachElement { ins ->
                if (ins.min < ins.max && ins.widenIfNecessary(irdata.labels, positionMap)) {
                    done = false
                }
            }
            if (done) {
                break
            }
        }

        jumpInstructions.forEachElement { ins ->
            assert(ins.min <= ins.max)
            ins.max = ins.min
        }
    }
}