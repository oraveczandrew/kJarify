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

import java.nio.charset.StandardCharsets

private interface IntIterator {
    fun next(): Int
    fun hasNext(): Boolean
}

private fun makeDecodeIterator(b: ByteArray): IntIterator {
    return object : IntIterator {
        private var index = 0

        override fun hasNext(): Boolean {
            return index < b.size
        }

        override fun next(): Int {
            if (!hasNext()) {
                throw NoSuchElementException()
            }
            val x = b[index++].toInt() and 0xFF
            if (x < 128) {
                return x
            } else {
                var extra = 0
                for (i in 6 downTo 1) {
                    if ((x and (1 shl i)) != 0) {
                        extra++
                    } else {
                        break
                    }
                }

                var bits = x % (1 shl (6 - extra))
                for (i in 0 until extra) {
                    bits = (bits shl 6) xor (b[index++].toInt() and 63)
                }
                return bits
            }
        }
    }
}

private fun fixPairs(codes: IntIterator): IntIterator {
    return object : IntIterator {
        override fun hasNext(): Boolean {
            return codes.hasNext()
        }

        override fun next(): Int {
            val x: Int = codes.next()
            if (0xD800 <= x && x < 0xDC00) {
                val high = x - 0xD800
                val low = codes.next() - 0xDC00
                return 0x10000 + (high shl 10) + (low and 1023)
            } else {
                return x
            }
        }
    }
}

internal fun decode(b: ByteArray): String {
    try {
        return String(b, StandardCharsets.UTF_8)
    } catch (_: Exception) {
        val result = StringBuilder()
        val codes = fixPairs(makeDecodeIterator(b))
        while (codes.hasNext()) {
            result.append(codes.next().toChar())
        }
        return result.toString()
    }
}