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

import java.util.function.BiFunction
import kotlin.math.min


internal class TreeList<T>(
    private val defaultValue: T,
    private val func: BiFunction<T, T, T>,
    data: TreeListSub<T>? = null
) {
    private var data: TreeListSub<T> = data ?: TreeListSub<T>(defaultValue)

    operator fun get(i: Int): T {
        return data.get(i)
    }

    operator fun set(i: Int, value: T) {
        data = data.set(i, value)
    }

    fun copy(): TreeList<T> {
        return TreeList(defaultValue, func, data)
    }

    fun merge(other: TreeList<T>) {
        require(func === other.func) { "Functions must be the same" }
        data = TreeListSub.merge(data, other.data, func)!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        val o = other as TreeList<*>

        if (defaultValue != o.defaultValue) return false
        if (func !== o.func) return false
        if (data != o.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultValue.hashCode()
        result = 31 * result + func.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }

    @Suppress("UNCHECKED_CAST")
    internal class TreeListSub<T>(
        @JvmField
        val defaultValue: T,
        @JvmField
        val direct: Array<T>,
        @JvmField
        val children: Array<TreeListSub<T>?>
    ) {

        constructor(defaultValue: T): this(
            defaultValue,
            direct = Array<Any>(SIZE) {
                defaultValue as Any
            } as Array<T>,
            children = arrayOfNulls(SPLIT) // Subtrees allocated lazily
        )

        fun get(i: Int): T {
            var i = i
            if (i < 0) {
                throw IndexOutOfBoundsException()
            }
            if (i < SIZE) {
                return direct[i]
            }

            i -= SIZE
            val childIndex = i % SPLIT
            i = i / SPLIT

            val child = children[childIndex]

            if (child == null) {
                return defaultValue
            }

            return child.get(i)
        }

        fun set(i: Int, value: T): TreeListSub<T> {
            var i = i
            if (i < 0) {
                throw IndexOutOfBoundsException()
            }
            if (i < SIZE) {
                if (direct[i] == value) {
                    return this
                }

                val temp = direct.clone()
                temp[i] = value
                return TreeListSub(defaultValue, temp, children)
            }

            i -= SIZE
            val childIndex = i % SPLIT
            i = i / SPLIT
            var child = children[childIndex]

            if (child == null) {
                if (value == defaultValue) {
                    return this
                }
                child = TreeListSub<T>(defaultValue).set(i, value)
            } else {
                if (value == child.get(i)) {
                    return this
                }
                child = child.set(i, value)
            }

            val temp = children.clone()
            temp[childIndex] = child
            return TreeListSub(defaultValue, direct, temp)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            val o = other as TreeListSub<*>

            if (defaultValue != o.defaultValue) return false
            if (!direct.contentEquals(o.direct)) return false
            if (!children.contentEquals(o.children)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = defaultValue.hashCode()
            result = 31 * result + direct.hashCode()
            result = 31 * result + children.hashCode()
            return result
        }

        companion object {
            const val SIZE = 16 // Example size
            const val SPLIT = 16 // Example split

            fun<T> merge(left: TreeListSub<T>?, right: TreeListSub<T>?, func: BiFunction<T, T, T>): TreeListSub<T>? {
                // Effectively computes [func(x, y) for x, y in zip(left, right)]
                // Assume func(x, x) == x
                if (left == right) {
                    return left
                }

                var left = left
                var right = right

                if (left == null) {
                    val temp = left
                    left = right
                    right = temp
                }

                val defaultVal = left!!.defaultValue
                val leftDirect = left.direct
                val leftChildren = left.children

                if (right == null) {
                    val direct: Array<T> = arrayOfNulls<Any>(SIZE) as Array<T>
                    for (i in leftDirect.indices) {
                        direct[i] = func.apply(leftDirect[i], defaultVal)
                    }

                    val children: Array<TreeListSub<T>?> = arrayOfNulls(SPLIT)
                    for (i in leftChildren.indices) {
                        val child = leftChildren[i]
                        val merged = merge(child, null, func)
                        if (merged != null) {
                            children[i] = merged
                        }
                    }

                    if (direct.contentEquals(leftDirect) && children.contentEquals(leftChildren)) {
                        return left
                    }

                    return TreeListSub<T>(defaultValue = defaultVal, direct = direct, children = children)
                } else {
                    val rightDirect = right.direct

                    val direct: Array<T> = arrayOfNulls<Any>(SIZE) as Array<T>
                    for (i in 0 until min(leftDirect.size, rightDirect.size)) {
                        direct[i] = func.apply(leftDirect[i], rightDirect[i])
                    }

                    val rightChildren = right.children

                    val children: Array<TreeListSub<T>?> = arrayOfNulls(SPLIT)
                    for (i in 0 until min(leftChildren.size, rightChildren.size)) {
                        val merged = merge(leftChildren[i], rightChildren[i], func)
                        if (merged != null) {
                            children[i] = merged
                        }
                    }

                    if (direct.contentEquals(leftDirect) && children.contentEquals(leftChildren)) {
                        return left
                    }

                    if (direct.contentEquals(rightDirect) && children.contentEquals(rightChildren)) {
                        return right
                    }

                    return TreeListSub<T>(defaultValue = defaultVal, direct = direct, children = children)
                }
            }
        }
    }
}
