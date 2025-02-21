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

import hu.oandras.kJarify.streams.Writer.Pool
import hu.oandras.kJarify.ushrIntoByte
import java.io.ByteArrayOutputStream

internal class Writer private constructor() {
    
    private val tempNumberBuffer: ByteArray = ByteArray(8)

    private val buf = ByteArrayOutputStream(8 * 1024)

    fun write(src: ByteArray) {
        buf.write(src)
    }

    fun u8(x: Int) {
        buf.write(x)
    }

    fun u16(x: Int) {
        val buffer = tempNumberBuffer

        buffer[0] = x.ushrIntoByte(8)
        buffer[1] = x.ushrIntoByte(0)

        buf.write(buffer, 0, 2)
    }

    fun u32(x: Int) {
        val buffer = tempNumberBuffer

        buffer[0] = x.ushrIntoByte(24)
        buffer[1] = x.ushrIntoByte(16)
        buffer[2] = x.ushrIntoByte(8)
        buffer[3] = x.ushrIntoByte(0)

        buf.write(buffer, 0, 4)
    }

    fun u64(x: Long) {
        val buffer = tempNumberBuffer

        buffer[0] = x.ushrIntoByte(56)
        buffer[1] = x.ushrIntoByte(48)
        buffer[2] = x.ushrIntoByte(40)
        buffer[3] = x.ushrIntoByte(32)
        buffer[4] = x.ushrIntoByte(24)
        buffer[5] = x.ushrIntoByte(16)
        buffer[6] = x.ushrIntoByte(8)
        buffer[7] = x.ushrIntoByte(0)

        buf.write(buffer, 0, 8)
    }
    
    fun toBytes(): ByteArray {
        return buf.toByteArray()
    }

    fun reset() {
        buf.reset()
    }

    @PublishedApi
    internal object Pool {
        private val pool: ArrayList<Writer> = ArrayList()

        private val lock: Any = Any()

        fun obtain(): Writer {
            return synchronized(lock) {
                pool.removeLastOrNull()
            } ?: Writer()
        }

        fun release(writer: Writer) {
            synchronized(lock) {
                pool.add(writer)
            }
        }
    }
}

internal inline fun withWriter(r: (Writer) -> Unit): ByteArray {
    val writer = Pool.obtain()
    r(writer)
    val bytes = writer.toBytes()
    writer.reset()
    Pool.release(writer)
    return bytes
}