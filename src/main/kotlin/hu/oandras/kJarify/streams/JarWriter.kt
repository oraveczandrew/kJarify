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

package hu.oandras.kJarify.streams

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class JarOutputStream private constructor(
    file: File,
) : ZipOutputStream(FileOutputStream(file)) {

    private val mutex: Mutex = Mutex()

    @Suppress("RemoveRedundantQualifierName")
    constructor(
        path: String,
        force: Boolean,
    ) : this(
        kotlin.run {
            val outfile = File(path)
            if (outfile.exists()) {
                if (!force) {
                    throw IOException("Output file exist: $path")
                } else {
                    outfile.delete()
                }
            }
            outfile
        }
    )

    suspend fun writeClass(unicodeName: String, data: ByteArray) {
        mutex.withLock {
            val info = ZipEntry(unicodeName)
            putNextEntry(info)
            write(data)
            closeEntry()
        }
    }
}