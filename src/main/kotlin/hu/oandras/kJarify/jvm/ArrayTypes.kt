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

internal object ArrayTypes {
    @JvmField
    val INVALID: ByteArray = "INVALID".toByteArray()

    @JvmField
    val NULL: ByteArray = "NULL".toByteArray()

    @JvmStatic
    fun merge(t1: ByteArray, t2: ByteArray): ByteArray {
        if (t1.contentEquals(NULL)) {
            return t2
        }
        if (t2.contentEquals(NULL)) {
            return t1
        }
        return if (t1.contentEquals(t2)) t1 else INVALID
    }

    @JvmStatic
    fun narrow(t1: ByteArray, t2: ByteArray): ByteArray {
        if (t1.contentEquals(INVALID)) {
            return t2
        }
        if (t2.contentEquals(INVALID)) {
            return t1
        }
        return if (t1.contentEquals(t2)) t1 else NULL
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun eletPair(t: ByteArray): EletPair {
        var t = t

        if (t.contentEquals(NULL)) {
            throw AssertionError()
        }

        if (t.contentEquals(INVALID)) {
            return EletPair(Scalars.OBJ, t)
        }

        if (t[0] != '['.toInt().toByte()) {
            throw AssertionError(t.decodeToString())
        }

        t = t.copyOfRange(1, t.size)

        return EletPair(Scalars.fromDesc(t), t)
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun fromDesc(desc: ByteArray): ByteArray {
        if (desc[0] != '['.toInt().toByte() || desc[desc.size - 1] == ';'.toInt().toByte()) {
            return INVALID
        }

        return desc
    }
}
