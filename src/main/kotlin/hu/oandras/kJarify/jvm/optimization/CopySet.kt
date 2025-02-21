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

import androidx.collection.ArraySet
import java.util.*

internal class CopySet<T> private constructor(
    @JvmField
    var root: T,
    private val set: MutableSet<T>,
    // keep track of insertion order in case root is overwritten
    private val q: LinkedList<T>
) {

    constructor(
        root: T
    ): this(
        root = root,
        set = HashSet(1),
        q = LinkedList(),
    )

    init {
        set.add(root)
    }

    fun add(key: T) {
        assert(set.isNotEmpty())
        set.add(key)
        q.add(key)
    }

    fun remove(key: T) {
        set.remove(key)
        // Heuristic - use the oldest element still in set as new root
        while (!q.isEmpty() && !set.contains(root)) {
            root = q.removeFirst()
        }
    }

    fun copy(): CopySet<T> {
        return CopySet<T>(
            root = root,
            set = if (set.isNotEmpty()) ArraySet(set) else ArraySet(),
            q = if (q.isNotEmpty()) LinkedList(q) else LinkedList(),
        )
    }
}