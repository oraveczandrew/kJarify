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

import androidx.collection.*

internal fun IntSet.toIntArray(): IntArray {
    val out = IntArray(size)
    var i = 0
    forEach {
        out[i] = it
        i++
    }
    return out
}

internal fun IntIntMap.keyArray(): IntArray {
    val out = IntArray(size)
    var i = 0
    forEachKey {
        out[i] = it
        i++
    }
    return out
}

internal fun<T> IntObjectMap<T>.keyArray(): IntArray {
    val out = IntArray(size)
    var i = 0
    forEachKey {
        out[i] = it
        i++
    }
    return out
}

internal fun<T> keysToRanges(d: MutableIntObjectMap<T>, limit: Int): IntObjectMap<T> {
    val starts = d.keyArray()
    starts.sort()

    for (i in starts.indices) {
        val s = starts[i]

        val e = if (i + 1 < starts.size) {
            starts[i + 1]
        } else {
            limit
        }

        val value = d[s]!!
        for (k in s until e) {
            d.put(k, value)
        }
    }

    return d
}

internal fun IntList.sum(): Int {
    var sum = 0
    for (i in indices) {
        sum += this[i]
    }
    return sum
}

internal inline fun <T> List<T>.forEachElement(r: (T) -> Unit) {
    for (i in 0 until size) {
        r.invoke(this[i])
    }
}

internal inline fun <T> List<T>.sumBy(selector: (T) -> Int): Int {
    var sum = 0
    forEachElement {
        sum += selector(it)
    }
    return sum
}