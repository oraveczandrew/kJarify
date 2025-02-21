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

import androidx.collection.IntSet
import androidx.collection.MutableIntSet
import androidx.collection.emptyIntSet
import hu.oandras.kJarify.streams.Stream

class CodeItem(dex: DexFile, offset: Int) {

    @JvmField
    val registerCount: Int

    @JvmField
    val instructionsSize: Int

    @JvmField
    val instructions: IntArray

    @JvmField
    val tries: Array<TryItem>

    @JvmField
    val listOffset: Int

    @JvmField
    val byteCode: Array<DalvikInstruction>

    init {
        val stream: Stream = dex.stream(offset)
        registerCount = stream.u16()
        @Suppress("unused")
        val insSize: Int = stream.u16()
        @Suppress("unused")
        val outsSize: Int = stream.u16()
        val triesSize: Int = stream.u16()
        @Suppress("unused")
        val debugOffset: Int = stream.u32()
        instructionsSize = stream.u32()
        val instructionsStartPos: Int = stream.pos()
        instructions = IntArray(instructionsSize) {
            stream.u16()
        }
        if (triesSize > 0 && (instructionsSize and 1) != 0) {
            stream.u16() // padding
        }
        tries = Array<TryItem>(triesSize) {
            TryItem(stream)
        }

        listOffset = stream.pos()
        for (item in tries) {
            item.finish(dex, listOffset)
        }

        val catchAddresses: IntSet
        if (tries.isNotEmpty()) {
            catchAddresses = MutableIntSet()
            for (tryItem in tries) {
                for (t in tryItem.catches) {
                    catchAddresses.add(t.pos)
                }
            }
        } else {
            catchAddresses = emptyIntSet()
        }

        byteCode = parseBytecode(
            dex = dex,
            instructionStartPosition = instructionsStartPos,
            shorts = instructions,
            catchAddresses = catchAddresses
        )
    }
}