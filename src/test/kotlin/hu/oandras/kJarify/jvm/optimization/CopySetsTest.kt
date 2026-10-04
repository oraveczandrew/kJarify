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

package hu.oandras.kJarify.jvm.optimization

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CopySetsTest {

    @Test
    fun `load of unknown key returns the key itself`() {
        val m = CopySetsMap<String>()
        assertEquals("a", m.load("a"))
        assertEquals("b", m.load("b"))
    }

    @Test
    fun `clobber of unknown key is a no-op`() {
        val m = CopySetsMap<String>()
        m.clobber("x") // must not throw or pollute the map
        assertEquals("x", m.load("x"))
    }

    @Test
    fun `move joins dest into src set with src root`() {
        val m = CopySetsMap<String>()
        assertTrue(m.move("d", "s"))
        assertEquals("s", m.load("s"))
        assertEquals("s", m.load("d"))
    }

    @Test
    fun `move within same set returns false`() {
        val m = CopySetsMap<String>()
        m.move("d", "s")
        assertFalse(m.move("d", "s"))
        assertFalse(m.move("s", "d"))
        assertFalse(m.move("s", "s"))
    }

    @Test
    fun `move of key onto itself returns false`() {
        val m = CopySetsMap<String>()
        assertFalse(m.move("a", "a"))
        assertEquals("a", m.load("a"))
    }

    @Test
    fun `move detaches dest from its old set`() {
        val m = CopySetsMap<String>()
        m.move("d", "x") // {x, d}, root x
        m.move("d", "s") // d joins {s}; old set {x} keeps root x
        assertEquals("s", m.load("d"))
        assertEquals("s", m.load("s"))
        assertEquals("x", m.load("x"))
    }

    @Test
    fun `clobber splits key out but keeps the rest`() {
        val m = CopySetsMap<String>()
        m.move("d", "s")
        m.clobber("d")
        assertEquals("d", m.load("d"))
        assertEquals("s", m.load("s"))
    }

    @Test
    fun `clobber of set root promotes oldest remaining`() {
        val m = CopySetsMap<String>()
        m.move("b", "a") // {a, b}
        m.move("c", "a") // {a, b, c}
        m.clobber("a")
        assertEquals("b", m.load("b"))
        assertEquals("b", m.load("c"))
        assertEquals("a", m.load("a"))
    }

    @Test
    fun `copy is an independent snapshot`() {
        val m = CopySetsMap<String>()
        m.move("d", "s")
        val c = m.copy()
        assertEquals(m.load("d"), c.load("d"))

        m.clobber("d")
        assertEquals("s", c.load("d")) // copy unaffected
        assertEquals("d", m.load("d"))

        m.move("e", "s")
        assertEquals("s", m.load("e"))
        assertEquals("s", c.load("s")) // copy's set has no e
        assertEquals("e", c.load("e")) // unknown in copy
    }

    @Test
    fun `copyset root heuristic picks oldest remaining`() {
        val s = CopySet("a")
        assertEquals("a", s.root)
        s.add("b")
        s.add("c")
        s.remove("b")
        assertEquals("a", s.root)
        s.remove("a")
        assertEquals("c", s.root) // b is gone, c is oldest remaining
        s.remove("nope") // removing a non-member is a no-op
        assertEquals("c", s.root)
    }

    @Test
    fun `copyset copy is independent`() {
        val s = CopySet("a")
        s.add("b")
        val c = s.copy()
        c.remove("a")
        assertEquals("b", c.root)
        assertEquals("a", s.root) // original untouched
        s.add("z")
        assertEquals("b", c.root) // copy untouched
    }
}
