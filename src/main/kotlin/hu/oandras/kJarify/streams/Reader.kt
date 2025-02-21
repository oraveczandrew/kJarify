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

package hu.oandras.kJarify.streams

import hu.oandras.kJarify.signExtend

internal class Reader @JvmOverloads constructor(
    private val data: ByteArray,
    private var pos: Int = 0
): Stream {

    override fun pos(): Int = pos

    override fun read(size: Int): ByteArray {
        if (size < 0 || size > data.size - pos) {
            throw IndexOutOfBoundsException()
        }
        val result = ByteArray(size)
        System.arraycopy(data, pos, result, 0, size)
        pos += size
        return result
    }

    private fun nextByte(): Byte {
        val d = data[pos]
        pos++
        return d
    }

    override fun u8(): Int {
        return nextByte().toInt() and 0xFF
    }

    private fun u8L(): Long {
        return nextByte().toLong() and 0xFF
    }

    override fun u16(): Int {
        return u8() or
                (u8() shl 8)
    }

    override fun u32(): Int {
        return u8() or
                (u8() shl 8) or
                (u8() shl 16) or
                (u8() shl 24)
    }

    override fun u64(): Long {
        return u8L() or
                (u8L() shl 8) or
                (u8L() shl 16) or
                (u8L() shl 24) or
                (u8L() shl 32) or
                (u8L() shl 40) or
                (u8L() shl 48) or
                (u8L() shl 56)
    }

    private fun leb128(signed: Boolean): Int {
        var result = 0
        var size = 0

        while (true) {
            val byte = data[pos].toInt()
            if ((byte and 0x80) == 0) {
                break
            }

            result = result xor ((byte and 0x7F) shl size)
            size += 7
            pos++
        }

        result = result xor ((data[pos].toInt() and 0x7F) shl size)
        size += 7
        pos++

        if (signed) {
            result = signExtend(result, size)
        }
        return result
    }

    override fun unsignedLeb128(): Int {
        return leb128(false)
    }

    override fun signedLeb128(): Int {
        return leb128(true)
    }

    // Maintain strings in binary encoding instead of attempting to decode them
    // since the output will be using the same encoding anyway
    fun readClassString(): ByteArray {
        val oldPos = pos
        while (pos < data.size && data[pos] != 0.toByte()) {
            pos++
        }
        val result = ByteArray(pos - oldPos)
        System.arraycopy(data, oldPos, result, 0, result.size)
        pos++ // Skip the null terminator
        return result
    }
}