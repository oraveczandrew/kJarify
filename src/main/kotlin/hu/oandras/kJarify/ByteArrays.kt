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

@file:Suppress("FunctionName")

package hu.oandras.kJarify

@Suppress("DEPRECATION")
internal fun ByteArray.startsWith(char: Char): Boolean {
    return isNotEmpty() && this[0] == char.toByte()
}

@Suppress("DEPRECATION")
internal fun ByteArray?.equalsWithChar(char: Char): Boolean {
    return this != null && size == 1 && this[0] == char.toByte()
}

internal fun concatenate(a: ByteArray, b: ByteArray): ByteArray {
    val result = ByteArray(a.size + b.size)
    System.arraycopy(a, 0, result, 0, a.size)
    System.arraycopy(b, 0, result, a.size, b.size)
    return result
}

internal fun concatenate(a: ByteArray, b: ByteArray, c: ByteArray): ByteArray {
    val result = ByteArray(a.size + b.size + c.size)
    a.copyInto(destination = result)
    b.copyInto(destination = result, destinationOffset = a.size)
    c.copyInto(destination = result, destinationOffset = a.size + b.size)
    return result
}

internal fun concatenate(a: ByteArray, b: ByteArray, c: ByteArray, d: ByteArray): ByteArray {
    val result = ByteArray(a.size + b.size + c.size + d.size)
    a.copyInto(destination = result)
    b.copyInto(destination = result, destinationOffset = a.size)
    c.copyInto(destination = result, destinationOffset = a.size + b.size)
    d.copyInto(destination = result, destinationOffset = a.size + b.size + c.size)
    return result
}

internal fun concatenate(a: ByteArray, b: ByteArray, c: ByteArray, d: ByteArray, e: ByteArray): ByteArray {
    val result = ByteArray(a.size + b.size + c.size + d.size + e.size)
    a.copyInto(destination = result)
    b.copyInto(destination = result, destinationOffset = a.size)
    c.copyInto(destination = result, destinationOffset = a.size + b.size)
    d.copyInto(destination = result, destinationOffset = a.size + b.size + c.size)
    e.copyInto(destination = result, destinationOffset = a.size + b.size + c.size + d.size)
    return result
}

internal fun concatenate(chunks: List<ByteArray>): ByteArray {
    val result = ByteArray(chunks.sumOf { it.size })

    var start = 0
    chunks.forEachElement { chunk ->
        chunk.copyInto(destination = result, destinationOffset = start)
        start += chunk.size
    }

    return result
}

internal fun byteArrayOf_u8(op: Int): ByteArray {
    return byteArrayOf(
        op.ushrIntoByte(0),
    )
}

internal fun byteArrayOf_u8u8(op: Int, x: Int): ByteArray {
    return byteArrayOf(
        op.ushrIntoByte(0),
        x.ushrIntoByte(0),
    )
}

internal fun byteArrayOf_u8u8u8(op: Int, x: Int, y: Int): ByteArray {
    return byteArrayOf(
        op.ushrIntoByte(0),
        x.ushrIntoByte(0),
        y.ushrIntoByte(0),
    )
}

internal fun byteArrayOf_u8u8u8u8(op: Int, x: Int, y: Int, z: Int): ByteArray {
    return byteArrayOf(
        op.ushrIntoByte(0),
        x.ushrIntoByte(0),
        y.ushrIntoByte(0),
        z.ushrIntoByte(0),
    )
}

internal fun byteArrayOf_u8u8u8u8u8(op: Int, x: Int, y: Int, z: Int, z2: Int): ByteArray {
    return byteArrayOf(
        op.ushrIntoByte(0),
        x.ushrIntoByte(0),
        y.ushrIntoByte(0),
        z.ushrIntoByte(0),
        z2.ushrIntoByte(0),
    )
}

internal fun byteArrayOf_u8u8u8u8u8u8(op: Int, x: Int, y: Int, z: Int, z2: Int, z3: Int): ByteArray {
    return byteArrayOf(
        op.ushrIntoByte(0),
        x.ushrIntoByte(0),
        y.ushrIntoByte(0),
        z.ushrIntoByte(0),
        z2.ushrIntoByte(0),
        z3.ushrIntoByte(0),
    )
}

internal fun byteArrayOf_u8u8u16(op: Int, x: Int, y: Int): ByteArray {
    return byteArrayOf(
        op.ushrIntoByte(0),
        x.ushrIntoByte(0),
        y.ushrIntoByte(8),
        y.ushrIntoByte(0),
    )
}

internal fun byteArrayOf_u8u16(op: Int, x: Int): ByteArray {
    return byteArrayOf(
        op.ushrIntoByte(0),
        x.ushrIntoByte(8),
        x.ushrIntoByte(0),
    )
}

internal fun byteArrayOf_u8u32(op: Int, x: Int): ByteArray {
    return byteArrayOf(
        op.ushrIntoByte(0),
        x.ushrIntoByte(24),
        x.ushrIntoByte(16),
        x.ushrIntoByte(8),
        x.ushrIntoByte(0),
    )
}

internal fun byteArrayOf_u8u16u8u8(op: Int, x: Int, y: Int, z: Int): ByteArray {
    return byteArrayOf(
        op.ushrIntoByte(0),
        x.ushrIntoByte(8),
        x.ushrIntoByte(0),
        y.ushrIntoByte(0),
        z.ushrIntoByte(0),
    )
}

internal fun byteArrayOf_u16u16u16u16(op: Int, x: Int, y: Int, z: Int): ByteArray {
    return byteArrayOf(
        op.ushrIntoByte(8),
        op.ushrIntoByte(0),
        x.ushrIntoByte(8),
        x.ushrIntoByte(0),
        y.ushrIntoByte(8),
        y.ushrIntoByte(0),
        z.ushrIntoByte(8),
        z.ushrIntoByte(0),
    )
}

internal fun byteArrayOf_u8u16u8u32(op: Int, x: Int, y: Int, z: Int): ByteArray {
    return byteArrayOf(
        op.ushrIntoByte(0),
        x.ushrIntoByte(8),
        x.ushrIntoByte(0),
        y.ushrIntoByte(0),
        z.ushrIntoByte(24),
        z.ushrIntoByte(16),
        z.ushrIntoByte(8),
        z.ushrIntoByte(0),
    )
}