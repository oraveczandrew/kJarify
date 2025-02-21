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

object Flags {
    const val ACC_PUBLIC = 0x1
    const val ACC_PRIVATE = 0x2
    const val ACC_PROTECTED = 0x4
    const val ACC_STATIC = 0x8
    const val ACC_FINAL = 0x10
    const val ACC_SYNCHRONIZED = 0x20
    const val ACC_VOLATILE = 0x40
    const val ACC_BRIDGE = 0x40
    const val ACC_TRANSIENT = 0x80
    const val ACC_VARARGS = 0x80
    const val ACC_NATIVE = 0x100
    const val ACC_INTERFACE = 0x200
    const val ACC_ABSTRACT = 0x400
    const val ACC_STRICT = 0x800
    const val ACC_SYNTHETIC = 0x1000
    const val ACC_ANNOTATION = 0x2000
    const val ACC_ENUM = 0x4000
    const val ACC_CONSTRUCTOR = 0x10000
    const val ACC_DECLARED_SYNCHRONIZED = 0x20000

    // Might as well include this for completeness even though modern JVMs ignore it
    const val ACC_SUPER = 0x20

    const val CLASS_FLAGS: Int = ACC_PUBLIC  or  ACC_FINAL or ACC_SUPER or ACC_INTERFACE or ACC_ABSTRACT or ACC_SYNTHETIC or ACC_ANNOTATION or ACC_ENUM
    const val FIELD_FLAGS: Int = ACC_PUBLIC or ACC_PRIVATE or ACC_PROTECTED or ACC_STATIC or ACC_FINAL or ACC_VOLATILE or ACC_TRANSIENT or ACC_SYNTHETIC or ACC_ENUM
    const val METHOD_FLAGS: Int = ACC_PUBLIC or ACC_PRIVATE or ACC_PROTECTED or ACC_STATIC or ACC_FINAL or ACC_SYNCHRONIZED or ACC_BRIDGE or ACC_VARARGS or ACC_NATIVE or ACC_ABSTRACT or ACC_STRICT or ACC_SYNTHETIC

    private val methodFlagArr: IntArray = intArrayOf(ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED, ACC_STATIC, ACC_FINAL, ACC_SYNCHRONIZED, ACC_BRIDGE, ACC_VARARGS, ACC_NATIVE, ACC_ABSTRACT, ACC_STRICT, ACC_SYNTHETIC)

    fun methodFlagsToString(flags: Int): String {
        val flags = flags and METHOD_FLAGS

        val builder = StringBuilder()

        for (flag in methodFlagArr) {
            if (flag and flags != 0) {
                builder.append(flagToString(flag))
                builder.append(", ")
            }
        }

        if (builder.isNotEmpty()) {
            builder.setLength(builder.length - 2)
        }

        return builder.toString()
    }

    private fun flagToString(flag: Int): String {
        return when (flag) {
            ACC_PUBLIC -> "ACC_PUBLIC"
            ACC_PRIVATE -> "ACC_PRIVATE"
            ACC_PROTECTED -> "ACC_PROTECTED"
            ACC_STATIC -> "ACC_STATIC"
            ACC_FINAL -> "ACC_FINAL"
            ACC_SYNCHRONIZED -> "ACC_SYNCHRONIZED"
            ACC_VARARGS -> "ACC_VARARGS"
            ACC_NATIVE -> "ACC_NATIVE"
            ACC_ABSTRACT -> "ACC_ABSTRACT"
            ACC_STRICT -> "ACC_STRICT"
            ACC_SYNTHETIC -> "ACC_SYNTHETIC"
            ACC_VOLATILE -> "ACC_VOLATILE"
            ACC_INTERFACE -> "ACC_INTERFACE"
            ACC_CONSTRUCTOR -> "ACC_CONSTRUCTOR"
            ACC_DECLARED_SYNCHRONIZED -> "ACC_DECLARED_SYNCHRONIZED"
            else -> "UNKNOWN"
        }
    }
}