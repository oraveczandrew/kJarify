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

@Suppress("DuplicatedCode")
internal class IntTreeList private constructor(
    private val defaultValue: Int,
    private val func: IntIntFunc,
    data: IntTreeListSub?
) {

    constructor(
        defaultValue: Int,
        func: IntIntFunc,
    ): this(
        defaultValue = defaultValue,
        func = func,
        data = null,
    )

    private var data: IntTreeListSub? = data

    operator fun get(i: Int): Int {
        return data?.get(i) ?: defaultValue
    }

    operator fun set(i: Int, value: Int) {
        data = (data ?: IntTreeListSub(defaultValue)).set(i, value)
    }

    fun copy(): IntTreeList {
        return IntTreeList(defaultValue, func, data)
    }

    fun merge(other: IntTreeList): Boolean {
        require(func === other.func) { "Functions must be the same" }
        val merged = IntTreeListSub.merge(data, other.data, func)
        if (merged === data) {
            return false
        }
        data = merged
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        val o = other as IntTreeList

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
    internal class IntTreeListSub(
        @JvmField
        val defaultValue: Int,
        @JvmField
        val direct: IntArray,
        @JvmField
        val children: Array<IntTreeListSub?>
    ) {

        constructor(defaultValue: Int): this(
            defaultValue,
            direct = IntArray(SIZE) {
                defaultValue
            },
            children = arrayOfNulls(SPLIT) // Subtrees allocated lazily
        )

        fun get(i: Int): Int {
            var i = i
            if (i < 0) {
                throw IndexOutOfBoundsException()
            }
            if (i < SIZE) {
                return direct[i]
            }

            i -= SIZE
            val childIndex = i % SPLIT
            i /= SPLIT

            val child = children[childIndex] ?: return defaultValue

            return child.get(i)
        }

        fun set(i: Int, value: Int): IntTreeListSub {
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
                return IntTreeListSub(defaultValue, temp, children)
            }

            i -= SIZE
            val childIndex = i % SPLIT
            i /= SPLIT
            var child = children[childIndex]

            if (child == null) {
                if (value == defaultValue) {
                    return this
                }
                child = IntTreeListSub(defaultValue).set(i, value)
            } else {
                if (value == child.get(i)) {
                    return this
                }
                child = child.set(i, value)
            }

            val temp = children.clone()
            temp[childIndex] = child
            return IntTreeListSub(defaultValue, direct, temp)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            val o = other as IntTreeListSub

            if (defaultValue != o.defaultValue) return false
            if (!direct.contentEquals(o.direct)) return false
            if (!children.contentEquals(o.children)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = defaultValue.hashCode()
            result = 31 * result + direct.contentHashCode()
            result = 31 * result + children.contentHashCode()
            return result
        }

        companion object {
            const val SIZE = 16 // Example size
            const val SPLIT = 16 // Example split

            fun merge(left: IntTreeListSub?, right: IntTreeListSub?, func: IntIntFunc): IntTreeListSub? {
                // Effectively computes [func(x, y) for x, y in zip(left, right)]
                // Assume func(x, x) == x
                // Single pass, allocates only on first actual difference (allocate-on-write).
                if (left === right) {
                    return left
                }
                if (left == null) {
                    return mergeWithDefault(right, func)
                }
                if (right == null) {
                    return mergeWithDefault(left, func)
                }

                val defaultVal = left.defaultValue
                val leftDirect = left.direct
                val rightDirect = right.direct

                var direct: IntArray? = null
                for (i in 0 until SIZE) {
                    val m = func.apply(leftDirect[i], rightDirect[i])
                    if (m != leftDirect[i]) {
                        var d = direct
                        if (d == null) {
                            d = leftDirect.clone()
                            direct = d
                        }
                        d[i] = m
                    }
                }

                val leftChildren = left.children
                val rightChildren = right.children

                var children: Array<IntTreeListSub?>? = null
                for (i in 0 until SPLIT) {
                    val merged = merge(leftChildren[i], rightChildren[i], func)
                    if (merged !== leftChildren[i]) {
                        var c = children
                        if (c == null) {
                            c = leftChildren.clone()
                            children = c
                        }
                        c[i] = merged
                    }
                }

                val d = direct
                val c = children
                if (d == null && c == null) {
                    return left
                }

                val newDirect = d ?: leftDirect
                val newChildren = c ?: leftChildren

                if (newDirect.contentEquals(rightDirect) && newChildren.contentEquals(rightChildren)) {
                    return right
                }

                return IntTreeListSub(defaultValue = defaultVal, direct = newDirect, children = newChildren)
            }

            private fun mergeWithDefault(node: IntTreeListSub?, func: IntIntFunc): IntTreeListSub? {
                if (node == null) {
                    return null
                }

                val defaultVal = node.defaultValue
                val nodeDirect = node.direct

                var direct: IntArray? = null
                for (i in 0 until SIZE) {
                    val m = func.apply(nodeDirect[i], defaultVal)
                    if (m != nodeDirect[i]) {
                        var d = direct
                        if (d == null) {
                            d = nodeDirect.clone()
                            direct = d
                        }
                        d[i] = m
                    }
                }

                val nodeChildren = node.children

                var children: Array<IntTreeListSub?>? = null
                for (i in 0 until SPLIT) {
                    val merged = mergeWithDefault(nodeChildren[i], func)
                    if (merged !== nodeChildren[i]) {
                        var c = children
                        if (c == null) {
                            c = nodeChildren.clone()
                            children = c
                        }
                        c[i] = merged
                    }
                }

                val d = direct
                val c = children
                if (d == null && c == null) {
                    return node
                }

                return IntTreeListSub(
                    defaultValue = defaultVal,
                    direct = d ?: nodeDirect,
                    children = c ?: nodeChildren,
                )
            }
        }
    }

    fun interface IntIntFunc {
        fun apply(left: Int, right: Int): Int
    }
}
