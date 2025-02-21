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

package hu.oandras.kJarify

import hu.oandras.kJarify.treeList.TreeList
import hu.oandras.kJarify.treeList.TreeList.TreeListSub
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.function.BiFunction

class TreeListTest {

    @Test
    fun testSet() {
        val t = TreeList<Int>(
            defaultValue = 0,
            func = Int::and,
        )

        for (i in 0 until 100 * TreeListSub.SIZE) {
            t[i] = i + 100
        }

        for (i in 0 until 100 * TreeListSub.SIZE) {
            assertEquals(i + 100, t[i])
        }
    }

    @Test
    fun testMutated() {
        val t = TreeList<Int>(
            defaultValue = 0,
            func = Int::and,
        )

        for (i in 0 until 100 * TreeListSub.SIZE) {
            t[i] = i + 100
        }

        val t2 = t.copy()
        t2[50] = 0

        assertEquals(0, t2[50])
        assertEquals(150, t[50])
    }

    @Test
    fun testEqualsEmpty() {
        val function: BiFunction<Int, Int, Int> = BiFunction { i1, i2 -> i1 and i2 }

        val t1 = TreeList<Int>(
            defaultValue = 0,
            func = function,
        )

        val t2 = TreeList<Int>(
            defaultValue = 0,
            func = function,
        )

        assertEquals(t1, t2)
    }

    @Test
    fun testEqualsCopied() {
        val t = TreeList<Int>(
            defaultValue = 0,
            func = Int::and,
        )

        for (i in 0 until 100 * TreeListSub.SIZE) {
            t[i] = i + 100
        }

        val t2 = t.copy()

        assertEquals(t, t2)
    }

    @Test
    fun testMerge() {
        val t = TreeList<Int>(
            defaultValue = 1,
            func = Int::and,
        )

        val t2 = t.copy()

        for (i in 0 until 100 * TreeListSub.SIZE) {
            t[i] = i + 100
        }

        for (i in 0 until 200 * TreeListSub.SIZE) {
            t2[i] = i + 1000
        }

        t.merge(t2)

        for (i in 0 until 100 * TreeListSub.SIZE) {
            assertEquals(i + 100 and i + 1000, t[i])
        }

        for (i in 100 * TreeListSub.SIZE until 200 * TreeListSub.SIZE) {
            assertEquals(i + 1000 and 1, t[i])
        }
    }


}