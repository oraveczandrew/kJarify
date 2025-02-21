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

@file:Suppress("unused", "SpellCheckingInspection")

package hu.oandras.kJarify

import hu.oandras.kJarify.jvm.JvmOps.ACONST_NULL
import hu.oandras.kJarify.jvm.JvmOps.DMUL
import hu.oandras.kJarify.jvm.JvmOps.DUP
import hu.oandras.kJarify.jvm.JvmOps.DUP2
import hu.oandras.kJarify.jvm.JvmOps.I2F
import hu.oandras.kJarify.jvm.JvmOps.I2L
import hu.oandras.kJarify.jvm.JvmOps.IXOR
import hu.oandras.kJarify.jvm.JvmOps.L2D
import hu.oandras.kJarify.jvm.JvmOps.LCONST_1
import hu.oandras.kJarify.jvm.JvmOps.POP
import hu.oandras.kJarify.jvm.JvmOps.POP2
import hu.oandras.kJarify.jvm.JvmOps.RETURN

@JvmField
internal val EmptyByteArray: ByteArray = byteArrayOf()

@JvmField
internal val EmptyIntArray = IntArray(0)

@JvmField
internal val JAVA_LANG_THROWABLE: ByteArray = "java/lang/Throwable".toByteArray()

@JvmField
internal val JAVA_LANG_OBJECT: ByteArray = "java/lang/Object".toByteArray()

@JvmField
internal val ARRAY_OF_JAVA_LANG_OBJECT: ByteArray = "[Ljava/lang/Object;".toByteArray()

@JvmField
internal val L_JAVA_LANG_OBJECT: ByteArray = "Ljava/lang/Throwable;".toByteArray()

@JvmField
internal val JAVA_LANG_STRING_BYTES: ByteArray = "Ljava/lang/String;".toByteArray()

@JvmField
internal val JAVA_LANG_CLASS_BYTES: ByteArray = "Ljava/lang/Class;".toByteArray()

@JvmField
internal val CODE: ByteArray = "Code".toByteArray()

@JvmField
internal val POP_BYTES: ByteArray = byteArrayOf(POP.toByte())

@JvmField
internal val POP2_BYTES: ByteArray = byteArrayOf(POP2.toByte())

@JvmField
internal val DUP_BYTES: ByteArray = byteArrayOf(DUP.toByte())

@JvmField
internal val DUP2_BYTES: ByteArray = byteArrayOf(DUP2.toByte())

@JvmField
internal val DMUL_BYTES: ByteArray = byteArrayOf(DMUL.toByte())

@JvmField
internal val ACONST_NULL_BYTES: ByteArray = byteArrayOf(ACONST_NULL.toByte())

@JvmField
internal val RETURN_BYTES: ByteArray = byteArrayOf(RETURN.toByte())

@JvmField
internal val L2D_BYTES: ByteArray = byteArrayOf(L2D.toByte())

@JvmField
internal val I2L_BYTES: ByteArray = byteArrayOf(I2L.toByte())

@JvmField
internal val I2F_BYTES: ByteArray = byteArrayOf(I2F.toByte())

@JvmField
internal val IXOR_BYTES: ByteArray = byteArrayOf(IXOR.toByte())

@JvmField
internal val LCONST_1_BYTES: ByteArray = byteArrayOf(LCONST_1.toByte())

@JvmField
internal val ConstantValueBytes: ByteArray = "ConstantValue".toByteArray()