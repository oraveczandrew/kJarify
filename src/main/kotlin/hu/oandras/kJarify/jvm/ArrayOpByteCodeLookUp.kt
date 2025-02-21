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

import androidx.collection.MutableObjectIntMap
import androidx.collection.ObjectIntMap
import hu.oandras.kJarify.byteArrayOf_u8
import hu.oandras.kJarify.byteArrayOf_u8u8


@Suppress("DEPRECATION")
internal object ArrayOpByteCodeLookUp {

    data class ByteArrayKey(
        @JvmField
        val data: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            val o =  other as ByteArrayKey

            return data.contentEquals(o.data)
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }

    @JvmField
    val newArrayCodes: ObjectIntMap<ByteArrayKey>

    @JvmField
    val arrStoreOps: ObjectIntMap<ByteArrayKey>

    @JvmField
    val arrLoadOps: ObjectIntMap<ByteArrayKey>

    init {
        val types = charArrayOf('Z', 'C', 'F', 'D', 'B', 'S', 'I', 'J')
        val newArrayCodes = MutableObjectIntMap<ByteArrayKey>(types.size)
        for (i in types.indices) {
            newArrayCodes.put(ByteArrayKey(byteArrayOf_u8u8('['.toInt(), types[i].toInt())), i + 4)
        }

        val storeTypes = charArrayOf('I', 'J', 'F', 'D', ' ', 'B', 'C', 'S')
        val arrStoreOps = MutableObjectIntMap<ByteArrayKey>(storeTypes.size)
        for (i in storeTypes.indices) {
            arrStoreOps.put(ByteArrayKey(byteArrayOf_u8(storeTypes[i].toInt())), JvmOps.IASTORE + i)
        }

        val loadTypes = charArrayOf('I', 'J', 'F', 'D', ' ', 'B', 'C', 'S')
        val arrLoadOps = MutableObjectIntMap<ByteArrayKey>(loadTypes.size)
        for (i in loadTypes.indices) {
            arrLoadOps.put(ByteArrayKey(byteArrayOf_u8(loadTypes[i].toInt())), JvmOps.IALOAD + i)
        }

        arrStoreOps.put(ByteArrayKey(byteArrayOf_u8('Z'.toInt())), JvmOps.BASTORE)
        arrLoadOps.put(ByteArrayKey(byteArrayOf_u8('Z'.toInt())), JvmOps.BALOAD)

        this.newArrayCodes = newArrayCodes
        this.arrStoreOps = arrStoreOps
        this.arrLoadOps = arrLoadOps
    }
}

