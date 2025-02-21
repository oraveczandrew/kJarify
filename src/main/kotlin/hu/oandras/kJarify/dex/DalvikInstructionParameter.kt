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

internal sealed interface DalvikInstructionParameter {

    data class IntegerParam(
        @JvmField
        val value: Int
    ): DalvikInstructionParameter {
        override fun toString(): String {
            return "I'$value"
        }
    }

    data class LongParam(
        @JvmField
        val value: Long,
    ): DalvikInstructionParameter {
        override fun toString(): String {
            return "L'$value"
        }
    }

    class LongArrayParam(
        @JvmField
        val value: LongArray,
    ): DalvikInstructionParameter {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LongArrayParam

            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }

        override fun toString(): String {
            return "[L'${value.contentToString()}"
        }
    }

    data class IntArrayParam(
        @JvmField
        val value: IntArray,
    ): DalvikInstructionParameter {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as IntArrayParam

            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }

        override fun toString(): String {
            return "[I'${value.contentToString()}"
        }
    }
}