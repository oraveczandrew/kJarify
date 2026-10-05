/*
 * Copyright (C) 2026 András Oravecz and the contributors.
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

package hu.oandras.kJarify

import hu.oandras.kJarify.treeList.IntTreeList
import hu.oandras.kJarify.treeList.TreeList
import org.junit.jupiter.api.Test
import java.util.function.BiFunction
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TreeListMergeFuzzTest {

    private companion object {
        // Covers direct (16), level 1 (272) and level 2 trie nodes
        const val MAX_INDEX = 600
    }

    // Number of trials where merge() reports changed=true although the
    // observable content is identical (e.g. func collapses everything to
    // default). Best-effort sharing only; legacy code behaves the same.
    private var spuriousChanged = 0
    private var totalMerges = 0

    private fun checkChangedFlag(leftBefore: List<Int>, left: IntTreeList, changed: Boolean, what: String) {
        totalMerges++
        val contentDiffers = leftBefore.indices.any { leftBefore[it] != left[it] }
        if (contentDiffers) {
            assertTrue(changed, "$what: real content change was not reported")
        } else {
            // content identical: changed=false is expected (structural sharing),
            // but changed=true is acceptable (e.g. empty merged with a tree
            // that func collapses to default) - count it, bound it below.
            if (changed) spuriousChanged++
        }
    }

    private val intFuncOps: List<(Int, Int) -> Int> = listOf(
        { a, b -> a and b },
        { a, b -> a or b },
        { a, b -> minOf(a, b) },
        { a, b -> maxOf(a, b) },
    )

    @Test
    fun intMergeMatchesReferenceModel() {
        val random = Random(1234)
        repeat(2000) { trial ->
            val op = intFuncOps[trial % intFuncOps.size]
            val func = IntTreeList.IntIntFunc { a, b -> op(a, b) }
            val default = if (trial % 2 == 0) 0 else -1
            
            val leftRef = randomSparseMap(random)
            val rightRef = randomSparseMap(random)

            val left = IntTreeList(default, func)
            leftRef.forEach { (k, v) -> left[k] = v }
            val right = IntTreeList(default, func)
            rightRef.forEach { (k, v) -> right[k] = v }

            val leftBefore = snapshot(left)
            val rightBefore = snapshot(right)

            val changed = left.merge(right)

            // reference: elementwise func, missing = default
            for (i in 0 until MAX_INDEX) {
                val expected = op(leftBefore[i], rightBefore[i])
                assertEquals(expected, left[i], "trial=$trial index=$i")
            }

            // 'other' must be untouched
            for (i in 0 until MAX_INDEX) {
                assertEquals(rightBefore[i], right[i], "trial=$trial: right mutated at $i")
            }

            // changed flag must reflect content difference vs old left
            checkChangedFlag(leftBefore, left, changed, "trial=$trial")

            // fixpoint: merging the result with right again must be a no-op
            val second = left.merge(right)
            assertFalse(second, "trial=$trial: second merge should be no-op")
        }

        assertTrue(
            spuriousChanged * 100.0 / totalMerges < 5.0,
            "too many spurious changed=true: $spuriousChanged/$totalMerges",
        )
    }

    @Test
    fun intMergeWithEmptyList() {
        val func = IntTreeList.IntIntFunc { a, b -> a and b }
        val random = Random(99)
        repeat(200) {
            val full = IntTreeList(0, func)
            val ref = randomSparseMap(random)
            ref.forEach { (k, v) -> full[k] = v }

            // empty.merge(full): mergeWithDefault path
            val empty = IntTreeList(0, func)
            val emptyBefore = snapshot(empty)
            val changedEmpty = empty.merge(full)
            checkChangedFlag(emptyBefore, empty, changedEmpty, "empty-vs-full")
            ref.forEach { (k, v) -> assertEquals(v and 0, empty[k]) }

            // full.merge(empty): other direction, func(x, default)
            val full2 = IntTreeList(0, func)
            ref.forEach { (k, v) -> full2[k] = v }
            val before = snapshot(full2)
            val changed = full2.merge(IntTreeList(0, func))
            checkChangedFlag(before, full2, changed, "full-vs-empty")
            for (i in 0 until 600) assertEquals(before[i] and 0, full2[i])

            // empty.merge(empty) is a no-op
            assertFalse(IntTreeList(0, func).merge(IntTreeList(0, func)))
        }
    }

    @Test
    fun genericMergeMatchesReferenceModel() {
        val ops: List<BiFunction<Int, Int, Int>> = listOf(
            BiFunction { a, b -> a and b },
            BiFunction { a, b -> a or b },
            BiFunction { a, b -> minOf(a, b) },
        )
        val random = Random(777)
        repeat(1000) { trial ->
            val op = ops[trial % ops.size]
            val default = 0
            
            val leftRef = randomSparseMap(random)
            val rightRef = randomSparseMap(random)

            val left = TreeList(default, op)
            leftRef.forEach { (k, v) -> left[k] = v }
            val right = TreeList(default, op)
            rightRef.forEach { (k, v) -> right[k] = v }

            val leftBefore = (0 until MAX_INDEX).map { left[it] }
            val rightBefore = (0 until MAX_INDEX).map { right[it] }

            val changed = left.merge(right)

            for (i in 0 until MAX_INDEX) {
                assertEquals(op.apply(leftBefore[i], rightBefore[i]), left[i], "trial=$trial index=$i")
            }
            for (i in 0 until MAX_INDEX) {
                assertEquals(rightBefore[i], right[i], "trial=$trial: right mutated at $i")
            }
            val contentDiffers = (0 until MAX_INDEX).any { leftBefore[it] != left[it] }
            if (contentDiffers) {
                assertTrue(changed, "trial=$trial: real content change was not reported")
            } else if (changed) {
                spuriousChanged++
            }
            totalMerges++
        }

        assertTrue(
            spuriousChanged * 100.0 / totalMerges < 5.0,
            "too many spurious changed=true: $spuriousChanged/$totalMerges",
        )
    }

    private fun randomSparseMap(random: Random): Map<Int, Int> {
        val n = random.nextInt(0, 60)
        val map = HashMap<Int, Int>()
        repeat(n) {
            // bias towards small indices but regularly cross level boundaries
            val k = if (random.nextDouble() < 0.7) random.nextInt(0, 40) else random.nextInt(0, MAX_INDEX)
            map[k] = random.nextInt(0, 8)
        }
        return map
    }

    private fun snapshot(list: IntTreeList): List<Int> =
        (0 until MAX_INDEX).map { list[it] }
}
