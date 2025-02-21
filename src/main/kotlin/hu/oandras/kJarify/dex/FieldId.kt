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

import hu.oandras.kJarify.streams.Stream

class FieldId(
    dex: DexFile,
    fieldIndex: Int
) : MFIdMixin() {

    override val className: ByteArray
    override val name: ByteArray
    override val descriptor: ByteArray

    init {
        val stream: Stream = dex.stream(dex.fieldIds.offset + fieldIndex * 8)
        className = dex.classType(stream.u16())
        descriptor = dex.type(stream.u16())
        name = dex.string(stream.u32())
    }

    override fun toString(): String {
        return "FieldId(cname='${className.decodeToString()}', name='${name.decodeToString()}', desc='${descriptor.decodeToString()}')"
    }
}