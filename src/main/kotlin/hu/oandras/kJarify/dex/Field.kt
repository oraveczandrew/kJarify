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

class Field(
    dex: DexFile,
    fieldIdx: Int,
    @JvmField
    val access: Int
) {
    @JvmField
    val id: FieldId = FieldId(dex, fieldIdx)

    @JvmField
    var constantValue: ConstantValue? = null // will be set later

    override fun toString(): String {
        return "Field(id=$id, access=[${Flags.methodFlagsToString(access)}], constantValue=${constantValue})"
    }
}