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

package hu.oandras.kJarify.dex

import hu.oandras.kJarify.JAVA_LANG_THROWABLE
import hu.oandras.kJarify.streams.Stream
import kotlin.math.abs

class TryItem internal constructor(stream: Stream) {

    @JvmField
    val start: Int = stream.u32()

    @JvmField
    val count: Int = stream.u16()

    @JvmField
    val handlerOff: Int = stream.u16()

    @JvmField
    val end: Int = start + count

    private var _catches: Array<CatchItem>? = null // to be filled in later

    val catches: Array<CatchItem>
        get() = _catches!!

    fun finish(dex: DexFile, listOff: Int) {
        val stream: Stream = dex.stream(listOff + handlerOff)
        val size: Int = stream.signedLeb128()

        val hasDefault = size <= 0
        val arrSize = abs(size)

        val catches = arrayOfNulls<CatchItem>(arrSize + if (hasDefault) 1 else 0)
        for (i in 0 until  arrSize) {
            catches[i] = CatchItem(dex.classType(stream.unsignedLeb128()), stream.unsignedLeb128())
        }

        if (hasDefault) {
            catches[arrSize] = CatchItem(JAVA_LANG_THROWABLE, stream.unsignedLeb128())
        }

        @Suppress("UNCHECKED_CAST")
        _catches = catches as Array<CatchItem>
    }
}