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

package hu.oandras.kJarify.treeList

internal fun Boolean.toInt() = if (this) 1 else 0

@Suppress("KotlinConstantConditions")
internal fun Int.toBoolean(): Boolean {
    return this == 1
}

internal class BooleanTreeList private constructor(
    private val defaultValue: Boolean,
    private val func: BooleanBooleanFunc,
    private val intTreeList: IntTreeList,
) {

    constructor(
        defaultValue: Boolean,
        func: BooleanBooleanFunc,
    ): this(
        defaultValue = defaultValue,
        func = func,
        intTreeList = IntTreeList(
            defaultValue.toInt()
        ) { val1, val2 ->
            func.apply(val1.toBoolean(), val2.toBoolean()).toInt()
        }
    )

    operator fun get(i: Int): Boolean {
        return intTreeList[i].toBoolean()
    }

    operator fun set(i: Int, value: Boolean) {
        intTreeList[i] = value.toInt()
    }

    fun copy(): BooleanTreeList {
        return BooleanTreeList(
            defaultValue = defaultValue,
            func = func,
            intTreeList = intTreeList.copy()
        )
    }

    fun merge(other: BooleanTreeList) {
        require(func === other.func) { "Functions must be the same" }
        intTreeList.merge(other.intTreeList)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BooleanTreeList

        if (defaultValue != other.defaultValue) return false
        if (func !== other.func) return false
        if (intTreeList != other.intTreeList) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultValue.hashCode()
        result = 31 * result + func.hashCode()
        result = 31 * result + intTreeList.hashCode()
        return result
    }


    fun interface BooleanBooleanFunc {
        fun apply(value1: Boolean, value2: Boolean): Boolean
    }
}