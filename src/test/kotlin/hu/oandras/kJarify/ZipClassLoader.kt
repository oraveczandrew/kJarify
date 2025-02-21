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

package hu.oandras.kJarify

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

class ZipClassLoader(file: File) : ClassLoader() {

    private val file: ZipFile = ZipFile(file)

    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*>? {
        val escaped = name.replace("/", ".")

        val fileName = name.replace(".", "/") + ".class"

        val entry = file.getEntry(fileName) ?: throw ClassNotFoundException(escaped)

        try {
            val array = ByteArray(1024)
            val input = file.getInputStream(entry)
            val out = ByteArrayOutputStream(array.size)
            var length = input.read(array)
            while (length > 0) {
                out.write(array, 0, length)
                length = input.read(array)
            }

            return defineClass(escaped, out.toByteArray(), 0, out.size())
        } catch (exception: IOException) {
            throw ClassNotFoundException(escaped, exception)
        }
    }
}