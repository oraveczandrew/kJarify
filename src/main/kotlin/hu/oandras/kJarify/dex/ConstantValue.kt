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

sealed interface ConstantValue {

    data class IntConstant(
        @JvmField
        val type: Char,
        @JvmField
        val value: Int,
    ): ConstantValue

    data class LongConstant(
        @JvmField
        val type: Char,
        @JvmField
        val value: Long,
    ): ConstantValue

    class ByteArrayConstant(
        @JvmField
        val type: String,
        @JvmField
        val value: ByteArray,
    ): ConstantValue {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ByteArrayConstant

            if (type != other.type) return false
            if (!value.contentEquals(other.value)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + value.contentHashCode()
            return result
        }

        override fun toString(): String {
            return "ByteArrayConstantValue(type='$type', value='${value.decodeToString()}')"
        }
    }

    class ConstantValueArrayConstant(
        @JvmField
        val array: Array<ConstantValue?>,
    ): ConstantValue {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ConstantValueArrayConstant

            return array.contentEquals(other.array)
        }

        override fun hashCode(): Int {
            return array.contentHashCode()
        }
    }
}