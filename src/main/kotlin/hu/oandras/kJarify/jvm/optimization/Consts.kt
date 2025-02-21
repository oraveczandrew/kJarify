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

import hu.oandras.kJarify.forEachElement
import hu.oandras.kJarify.jvm.ConstantPool
import hu.oandras.kJarify.jvm.IRWriter
import hu.oandras.kJarify.jvm.JvmInstruction.PrimitiveConstant
import hu.oandras.kJarify.jvm.PoolData
import hu.oandras.kJarify.jvm.PoolData.IntData
import hu.oandras.kJarify.jvm.Scalars.isWide
import kotlin.math.min

internal object Consts {

    fun allocateRequiredConstants(pool: ConstantPool, longIrs: List<IRWriter>) {
        // We allocate the constants pretty much greedily. This is far from optimal,
        // but it shouldn't be a big deal since this code is almost never required
        // in the first place. In fact, there are no known real world classes that
        // even come close to exhausting the constant pool.
        val narrowPairs: HashMap<PoolData, Int> = HashMap()
        val widePairs: HashMap<PoolData, Int> = HashMap()
        val altLens: HashMap<PoolData, Int> = HashMap()

        longIrs.forEachElement { ir ->
            ir.flatInstructions!!.forEachElement { ins ->
                if (ins is PrimitiveConstant) {
                    val bytecode = ins.bytecode
                    val key = ins.getConstantPoolData()
                    altLens.put(key, bytecode.size)
                    if (isWide(ins.staticType)) {
                        if (bytecode.size > 3) {
                            widePairs.put(key, widePairs.getOrDefault(key, 0) + 1)
                        }
                    } else {
                        if (bytecode.size > 2) {
                            narrowPairs.put(key, narrowPairs.getOrDefault(key, 0) + 1)
                        }
                    }
                }
            }
        }

        // see if already in the constant pool
        pool.values.forEachElement { x ->
            narrowPairs.remove(x)
            widePairs.remove(x)
        }

        // if we have enough space for all required constants, preferentially allocate
        // most commonly used constants to first 255 slots
        if (
            pool.space() >= (narrowPairs.size + 2 * widePairs.size) &&
            pool.lowSpace() > 0
        ) {
            val mostCommon: ArrayList<PoolDataScorePair> = narrowPairs.entries.mapTo(ArrayList(narrowPairs.entries.size)) {
                PoolDataScorePair(it.key, it.value)
            }
            mostCommon.sort()

            for (i in 0 until min(pool.lowSpace(), mostCommon.size)) {
                val poolData = mostCommon[i].poolData
                pool.insertDirectly(poolData, true)
                narrowPairs.remove(poolData)
            }
        }

        val scores: HashMap<PoolData, Int> = HashMap(narrowPairs.size + widePairs.size)

        for (entry in narrowPairs.entries) {
            val p = entry.key
            val count: Int = entry.value
            scores.put(p, (altLens[p]!! - 3) * count)
        }

        for (entry in widePairs.entries) {
            val p = entry.key
            val count: Int = entry.value
            scores.put(p, (altLens[p]!! - 3) * count)
        }

        // sort by score
        val narrowQ: ArrayList<PoolDataScorePair> = narrowPairs.entries.mapTo(ArrayList(narrowPairs.entries.size)) {
            PoolDataScorePair(
                poolData = it.key,
                score = -scores[it.key]!!
            )
        }
        narrowQ.sort()

        val wideQ: ArrayList<PoolDataScorePair> = widePairs.entries.mapTo(ArrayList(widePairs.entries.size)) {
            PoolDataScorePair(
                poolData = it.key,
                score = -scores[it.key]!!
            )
        }
        wideQ.sort()

        while (pool.space() >= 1 && (narrowQ.isNotEmpty() || wideQ.isNotEmpty())) {
            if (narrowQ.isEmpty() && pool.space() < 2) {
                break
            }

            val wScore: Int = if (wideQ.isEmpty()) 0 else scores[wideQ.last().poolData]!!

            val nScore: Int = when (val narrowQSize = narrowQ.size) {
                1 -> {
                    scores[narrowQ[0].poolData]!!
                }
                0 -> {
                    0
                }
                else -> {
                    scores[narrowQ[narrowQSize - 1].poolData]!! + scores[narrowQ[narrowQSize - 2].poolData]!!
                }
            }

            if (pool.space() >= 2 && wScore > nScore && wScore > 0) {
                pool.insertDirectly(wideQ.removeAt(wideQ.lastIndex).poolData, false)
            } else if (nScore > 0) {
                pool.insertDirectly(narrowQ.removeAt(narrowQ.lastIndex).poolData, true)
            } else {
                break
            }
        }
    }

    class PoolDataScorePair(
        @JvmField
        val poolData: PoolData,
        @JvmField
        val score: Int,
    ): Comparable<PoolDataScorePair> {

        private val poolValue: ULong = if (poolData is PoolData.LongData) poolData.value.toULong() else (poolData as IntData).value.toULong()

        override fun compareTo(other: PoolDataScorePair): Int {
            val result = -score.compareTo(other.score)
            return if (result != 0) {
                result
            } else {
                poolValue.compareTo(other.poolValue)
            }
        }

        override fun toString(): String {
            return "PoolDataScorePair(poolData=$poolData, score=$score, poolValue=$poolValue)"
        }
    }
}
