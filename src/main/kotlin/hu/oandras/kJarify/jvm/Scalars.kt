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

package hu.oandras.kJarify.jvm

import androidx.collection.IntIntMap
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntObjectMap
import hu.oandras.kJarify.dex.MethodId

internal object Scalars {
    const val INVALID = 0
    const val INT: Int = 1 shl 0
    const val FLOAT: Int = 1 shl 1
    const val OBJ: Int = 1 shl 2
    const val LONG: Int = 1 shl 3
    const val DOUBLE: Int = 1 shl 4

    const val ZERO: Int = INT or FLOAT or OBJ
    const val C32: Int = INT or FLOAT
    const val C64: Int = LONG or DOUBLE
    const val ALL: Int = ZERO or C64

    @Suppress("DEPRECATION")
    private val descToScalar: IntIntMap = MutableIntIntMap().apply {
        put('Z'.toInt(), INT)
        put('B'.toInt(), INT)
        put('C'.toInt(), INT)
        put('S'.toInt(), INT)
        put('I'.toInt(), INT)
        put('F'.toInt(), FLOAT)
        put('J'.toInt(), LONG)
        put('D'.toInt(), DOUBLE)
        put('L'.toInt(), OBJ)
        put('['.toInt(), OBJ)
    }

    @Suppress("DEPRECATION")
    private val descToStr: IntObjectMap<String> = MutableIntObjectMap<String>().apply {
        put(INT, "Int")
        put(FLOAT, "Float")
        put(LONG, "Long")
        put(DOUBLE, "Double")
        put(OBJ, "Object")
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun fromDesc(desc: ByteArray): Int {
        return descToScalar[desc[0].toInt()]
    }

    @JvmStatic
    fun staticTypeToStr(st: Int): String {
        return descToStr.getOrElse(st) {
            error("static type not found: $st")
        }
    }

    @JvmStatic
    fun isWide(st: Int): Boolean {
        return (st and C64) != 0
    }

    @JvmStatic
    fun paramTypes(methodId: MethodId, isStatic: Boolean): IntArray {
        val tempArr: Array<ByteArray?> = methodId.getSpacedParameterTypes(isStatic)

        return IntArray(tempArr.size) { i ->
            val temp = tempArr[i]
            if (temp == null) {
                INVALID
            } else {
                fromDesc(temp)
            }
        }
    }
}

