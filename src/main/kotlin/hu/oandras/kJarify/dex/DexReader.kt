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

import hu.oandras.kJarify.streams.readFile
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

interface DexReader {
    fun read(filePath: String): List<ByteArray>

    object SimpleDexFileReader: DexReader {

        override fun read(filePath: String): List<ByteArray> {
            val dexList: ArrayList<ByteArray> = ArrayList()

            try {
                dexList.add(readFile(filePath))
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return dexList
        }
    }

    object ApkDexFileReader : DexReader {
        override fun read(filePath: String): List<ByteArray> {
            val dexList: MutableList<ByteArray> = ArrayList<ByteArray>()

            try {
                ZipFile(
                    File(filePath),
                    ZipFile.OPEN_READ,
                ).use { z ->
                    val entries = z.entries()
                    while (entries.hasMoreElements()) {
                        val entry: ZipEntry = entries.nextElement()
                        if (entry.getName().startsWith("classes") && entry.getName().endsWith(".dex")) {
                            dexList.add(z.getInputStream(entry).readBytes())
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return dexList
        }
    }
}