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
import androidx.collection.MutableIntList
import hu.oandras.kJarify.forEachElement
import hu.oandras.kJarify.jvm.IRWriter
import hu.oandras.kJarify.jvm.JvmInstruction
import hu.oandras.kJarify.jvm.JvmInstruction.*

internal object Dup2izeOptimization: JvmOptimization() {

    private fun makeRange(instr: RegistryAccess?): UseRange {
        assert(instr is RegistryAccess && !instr.store)
        return UseRange(MutableIntList())
    }

    // Range of instruction indexes at which a given register is read (in linear code)
    private class UseRange(
        @JvmField
        val uses: MutableIntList
    ) {
        fun add(i: Int) {
            uses.add(i)
        }

        fun start(): Int {
            return uses[0]
        }

        fun end(): Int {
            return uses[uses.size - 1]
        }

        fun subtract(other: UseRange): List<UseRange> {
            val s = other.start()
            val e = other.end()
            val left = MutableIntList(uses.size)
            val right = MutableIntList(uses.size)

            val uses = uses
            for (k in uses.indices) {
                val i = uses[k]
                if (i < s) {
                    left.add(i)
                } else if (i > e) {
                    right.add(i)
                }
            }

            val result: ArrayList<UseRange> = ArrayList()

            if (left.size >= 2) {
                result.add(UseRange(left))
            }

            if (right.size >= 2) {
                result.add(UseRange(right))
            }

            return result
        }

        fun sortKey(): Int {
            return uses.size * 1000 + uses[0] // Arbitrary multiplier for sorting
        }
    }

    override fun optimize(irdata: IRWriter) {
        // This optimization replaces narrow registers which are frequently read at
        // stack height 0 with a single read followed by the more efficient dup and
        // dup2 instructions. This asymptotically uses only half a byte per access.
        // For simplicity, instead of explicitly keeping track of which locations
        // have stack height 0, we take advantage of the invariant that ranges of code
        // corresponding to a single Dalvik instruction always begin with empty stack.
        // These can be recognized by labels with a non-None id.
        // This isn't true for move-result instructions, but in that case the range
        // won't begin with a register load so it doesn't matter.
        // Note that pruneStoreLoads breaks this invariant, so dup2ize must be run first.
        // Also, for simplicity, we only keep at most one such value on the stack at
        // a time (duplicated up to 4 times).
        val instructions: List<JvmInstruction> = irdata.flatInstructions!!

        var ranges: ArrayList<UseRange> = ArrayList()
        val current: ArrayMap<RegistryAccess.Key, UseRange> = ArrayMap()
        var atHead = false

        for (i in instructions.indices) {
            val instr: JvmInstruction = instructions[i]
            // if not linear section of bytecode, reset everything. Exceptions are ok
            // since they clear the stack, but jumps obviously aren't.
            if (irdata.isJumpTarget(instr) || instr is If || instr is Switch) {
                ranges.addAll(current.values)
                current.clear()
            }

            if (instr is RegistryAccess) {
                val regAccess = instr
                val key = regAccess.key
                if (!regAccess.wide) {
                    if (regAccess.store) {
                        if (current.containsKey(key)) {
                            ranges.add(current.remove(key)!!)
                        }
                    } else if (atHead) {
                        // putIfAbsent
                        current.getOrPut(key) {
                            makeRange(regAccess)
                        }.add(i)
                    }
                }
            }

            atHead = instr is Label && instr.id != null
        }

        ranges.addAll(current.values)
        ranges.removeAll { ur: UseRange -> ur.uses.size < 2 }
        ranges.sortWith { ur1, ur2 ->
            ur1.sortKey().compareTo(ur2.sortKey())
        }

        // Greedily choose a set of disjoint ranges to dup2ize.
        val chosen = ArrayList<UseRange>(ranges.size)
        while (!ranges.isEmpty()) {
            val best = ranges.removeAt(ranges.lastIndex)
            chosen.add(best)
            val newRanges = ArrayList<UseRange>(ranges.size * 2)
            ranges.forEachElement { ur ->
                newRanges.addAll(ur.subtract(best))
            }
            ranges = newRanges
            ranges.sortWith { ur1: UseRange, ur2: UseRange ->
                ur1.sortKey().compareTo(ur2.sortKey())
            }
        }

        val replace = ArrayMap<JvmInstruction, List<JvmInstruction>>()
        chosen.forEachElement { ur ->
            val uses = ur.uses
            val gen = genDups(uses.size, 0)
            for (i in uses.indices) {
                val pos = uses[i]
                val ops = gen.next()
                // remember to include initial load!
                if (pos == ur.start()) {
                    val initialOps = ArrayList<JvmInstruction>(1 + ops.size)
                    initialOps.add(instructions[pos])
                    initialOps.addAll(ops)
                    replace.put(instructions[pos], initialOps)
                } else {
                    replace.put(instructions[pos], ops)
                }
            }
        }

        irdata.replaceInstructions(replace)
    }

    // used by writeir too
    fun genDups(needed: Int, neededAfter: Int): Iterator<List<JvmInstruction>> {
        // Generate a sequence of dup and dup2 instructions to duplicate the given
        // value. This keeps up to 4 copies of the value on the stack. Thanks to dup2
        // this asymptotically takes only half a byte per access.
        var needed = needed
        var have = 1
        val eleCount = needed
        needed += neededAfter

        val result = ArrayList<List<JvmInstruction>>(needed)

        @Suppress("unused")
        for (i in 0 until eleCount) {
            val cur = if (have < needed) {
                val cur = ArrayList<JvmInstruction>(2)
                if (have == 1 && needed >= 2) {
                    cur.add(Dup())
                    have += 1
                }

                if (have == 2 && needed >= 4) {
                    cur.add(Dup2())
                    have += 2
                }
                cur
            } else {
                emptyList()
            }

            have -= 1
            needed -= 1
            result.add(cur)
        }
        assert(have >= needed)

        // check if we have to pop at end
        val popCount = have - needed
        val pops = Array<JvmInstruction>(popCount) {
            Pop()
        }
        result.add(pops.toList())

        return result.iterator()
    }
}
