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

import hu.oandras.kJarify.jvm.IRWriter
import hu.oandras.kJarify.jvm.JvmInstruction

abstract class JvmOptimization {

    interface Visitor {
        fun visitExceptionRange()
        fun visitJumpTargetOrBranch(instr: JvmInstruction?)
        fun visitReturn() {}
        fun visit(instr: JvmInstruction) {}
    }

    open class NoExceptVisitorBase : Visitor {
        override fun visitExceptionRange() {
            reset()
        }

        override fun visitJumpTargetOrBranch(instr: JvmInstruction?) {
            reset()
        }

        open fun reset() {
            // Reset logic here
        }
    }

    protected fun <T : Visitor> visitLinearCode(irdata: IRWriter, visitor: T): T {
        var exceptLevel = 0
        val flatInstructions = irdata.flatInstructions!!
        for (i in flatInstructions.indices) {
            val instr = flatInstructions[i]

            if (irdata.isExceptionStart(instr)) {
                exceptLevel++
                visitor.visitExceptionRange()
            } else if (irdata.isExceptionEnd(instr)) {
                exceptLevel--
            }

            if (exceptLevel > 0) {
                continue
            }

            if (irdata.jumpTargets.contains(instr) || instr is JvmInstruction.LazyJumpBase || instr is JvmInstruction.Switch) {
                visitor.visitJumpTargetOrBranch(instr)
            } else if (!instr.fallsThrough()) {
                visitor.visitReturn()
            } else {
                visitor.visit(instr)
            }
        }
        assert(exceptLevel == 0)
        return visitor
    }

    abstract fun optimize(irdata: IRWriter)
}