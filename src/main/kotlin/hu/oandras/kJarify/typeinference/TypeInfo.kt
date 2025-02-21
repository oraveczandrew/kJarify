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

package hu.oandras.kJarify.typeinference

import hu.oandras.kJarify.jvm.ArrayTypes
import hu.oandras.kJarify.jvm.Scalars
import hu.oandras.kJarify.treeList.BooleanTreeList
import hu.oandras.kJarify.treeList.IntTreeList
import hu.oandras.kJarify.treeList.TreeList

internal class TypeInfo(
    @JvmField
    val prims: IntTreeList,
    @JvmField
    val arrays: TreeList<ByteArray>,
    @JvmField
    val tainted: BooleanTreeList,
) {
    fun copy(): TypeInfo {
        return TypeInfo(prims.copy(), arrays.copy(), tainted.copy())
    }

    fun set(registry: Int, st: Int, at: ByteArray, taint: Boolean = false): TypeInfo {
        prims[registry] = st
        arrays[registry] = at
        tainted[registry] = taint
        return this
    }

    fun move(src: Int, dest: Int, wide: Boolean): TypeInfo {
        val newTypeInfo = copy().set(
            registry = dest,
            st = prims[src],
            at = arrays[src],
            taint = tainted[src],
        )

        if (wide) {
            newTypeInfo.set(
                registry = dest + 1,
                st = prims[src + 1],
                at = arrays[src + 1],
                taint = tainted[src + 1],
            )
        }

        return newTypeInfo
    }

    fun assign(reg: Int, st: Int, at: ByteArray = ArrayTypes.INVALID, taint: Boolean = false): TypeInfo {
        return copy().set(reg, st, at, taint)
    }

    fun assign2(reg: Int, st: Int): TypeInfo {
        val at = ArrayTypes.INVALID
        return copy()
            .set(reg, st, at)
            .set(reg + 1, Scalars.INVALID, at)
    }

    fun assignFromDesc(reg: Int, desc: ByteArray): TypeInfo {
        val st: Int = Scalars.fromDesc(desc)
        val at: ByteArray = ArrayTypes.fromDesc(desc)
        return if (Scalars.isWide(st)) {
            assign2(reg, st)
        } else {
            assign(reg, st, at)
        }
    }

    fun isSame(other: TypeInfo): Boolean {
        return prims == other.prims && arrays == other.arrays && tainted == other.tainted
    }
}