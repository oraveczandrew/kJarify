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

import hu.oandras.kJarify.dex.DexProcessor
import hu.oandras.kJarify.dex.DexReader
import hu.oandras.kJarify.jvm.optimization.OptimizationOptions
import hu.oandras.kJarify.streams.JarOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.zip.Deflater

@Suppress("unused")
object KJarify {

    @JvmOverloads
    @JvmStatic
    fun process(
        input: File,
        output: File,
        optimizationOptions: OptimizationOptions,
        jarCompressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
    ) = runBlocking(Dispatchers.Unconfined) {
        suspendProcess(
            input = input,
            output = output,
            optimizationOptions = optimizationOptions,
            jarCompressionLevel = jarCompressionLevel,
        )
    }

    suspend fun suspendProcess(
        input: File,
        output: File,
        optimizationOptions: OptimizationOptions,
        jarCompressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
    ) {
        val jarWriter = JarOutputStream(
            path = output.absolutePath,
            force = true,
            compressionLevel = jarCompressionLevel,
        )

        val inputFile = input.absolutePath
        val dexReader = if (inputFile.endsWith(".apk", ignoreCase = true)) {
            DexReader.ApkDexFileReader
        } else {
            DexReader.SimpleDexFileReader
        }

        val dexDataList = dexReader.read(filePath = inputFile)

        val callback = object : DexProcessor.SysOutProcessStatusCallBack() {
            override suspend fun suspendOnClassTranslated(unicodeRelativePath: String, classData: ByteArray) {
                jarWriter.writeClass(unicodeRelativePath, classData)
            }
        }

        val processor = DexProcessor(
            optimizationOptions = optimizationOptions,
            coroutineDispatcher = Dispatchers.Default,
            callback = callback,
        )

        processor.suspendProcess(dexDataList)
    }
}