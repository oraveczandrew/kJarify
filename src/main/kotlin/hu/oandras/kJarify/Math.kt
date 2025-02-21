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

@file:Suppress("NOTHING_TO_INLINE")

package hu.oandras.kJarify

internal fun signExtend(value: Int, size: Int): Int {
    var x = value.toUInt()
    if ((x and (1u shl (size - 1))) != 0u) {
        x -= (1u shl size)
    }
    return x.toInt()
}

internal fun s16(value: Int): Int {
    var value = value.toUInt()
    value %= 1u shl 16
    if (value >= 1u shl 15) {
        value -= 1u shl 16
    }
    return value.toInt()
}

internal inline fun Int.ushrIntoByte(shift: Int): Byte {
    if (shift == 0) {
        return toByte()
    }

    return ushrIntoByteImpl(this, shift)
}

internal inline fun ushrIntoByteImpl(n: Int, shift: Int): Byte {
    return (n ushr shift and 0xFF).toByte()
}

internal inline fun Long.ushrIntoByte(shift: Int): Byte {
    if (shift == 0) {
        return toByte()
    }

    return ushrIntoByteImpl(this, shift)
}

internal inline fun ushrIntoByteImpl(n: Long, shift: Int): Byte {
    return (n ushr shift and 0xFFL).toByte()
}