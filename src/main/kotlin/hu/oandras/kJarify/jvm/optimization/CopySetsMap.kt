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

internal class CopySetsMap<T> {
    private val lookup: MutableMap<T, CopySet<T>> = HashMap()

    private fun get(key: T): CopySet<T> {
        return lookup.getOrPut(key) {
            CopySet<T>(key)
        }
    }

    fun clobber(key: T) {
        get(key).remove(key)
        lookup.remove(key)
    }

    fun move(dest: T, src: T): Boolean {
        // return false if the corresponding instructions should be removed
        val sSet = get(src)
        val dSet = get(dest)
        if (sSet === dSet) {
            // src and dest are copies of same value, so we can remove
            return false
        }
        dSet.remove(dest)
        sSet.add(dest)
        lookup.put(dest, sSet)
        return true
    }

    fun load(key: T): T {
        return get(key).root
    }

    fun copy(): CopySetsMap<T> {
        val copies: HashMap<CopySet<T>, CopySet<T>> = HashMap()
        val newMap = CopySetsMap<T>()

        for (entry in lookup.entries) {
            val v = entry.value

            val copy = copies.getOrPut(v) {
                v.copy()
            }

            newMap.lookup.put(entry.key, copy)
        }

        return newMap
    }
}