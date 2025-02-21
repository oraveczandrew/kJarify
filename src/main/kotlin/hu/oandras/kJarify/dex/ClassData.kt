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

class ClassData(dex: DexFile, offset: Int) {
    @JvmField
    val fields: List<Field>

    @JvmField
    val methods: List<Method>

    init {
        // for offset 0, leave dummy data with no fields or methods
        if (offset == 0) {
            this.fields = emptyList()
            this.methods = emptyList()
        } else {
            val stream = dex.stream(offset)

            val numStatic: Int = stream.unsignedLeb128()
            val numInstance: Int = stream.unsignedLeb128()
            val numDirect: Int = stream.unsignedLeb128()
            val numVirtual: Int = stream.unsignedLeb128()

            val fields = ArrayList<Field>(numStatic + numInstance)
            addFields(dex, stream, numStatic, fields)
            addFields(dex, stream, numInstance, fields)
            this.fields = fields

            val methods = ArrayList<Method>(numDirect + numVirtual)
            addMethods(dex, stream, numDirect, methods)
            addMethods(dex, stream, numVirtual, methods)
            this.methods = methods
        }
    }

    private fun addFields(dex: DexFile, stream: Stream, num: Int, fields: MutableList<Field>) {
        var fieldIndex = 0
        for (i in 0 until num) {
            fieldIndex += stream.unsignedLeb128()
            fields.add(Field(dex, fieldIndex, stream.unsignedLeb128()))
        }
    }

    private fun addMethods(dex: DexFile, stream: Stream, num: Int, methods: MutableList<Method>) {
        var methodIndex = 0
        for (i in 0 until num) {
            methodIndex += stream.unsignedLeb128()
            methods.add(Method(
                dex = dex,
                methodIndex = methodIndex,
                access = stream.unsignedLeb128(),
                codeOffset = stream.unsignedLeb128()
            ))
        }
    }
}