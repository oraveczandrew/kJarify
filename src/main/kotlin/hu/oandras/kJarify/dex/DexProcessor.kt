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

import hu.oandras.kJarify.streams.decode
import hu.oandras.kJarify.defaultThreadPool
import hu.oandras.kJarify.jvm.JVMClassWriter
import hu.oandras.kJarify.jvm.optimization.OptimizationOptions
import hu.oandras.kJarify.suspendMapTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executor

class DexProcessor(
    private val optimizationOptions: OptimizationOptions = OptimizationOptions.PRETTY,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val callback: ProcessCallBack = SysOutProcessStatusCallBack(),
    private val allowErrors: Boolean = true,
) {

    @Suppress("unused")
    @JvmOverloads
    constructor(
        optimizationOptions: OptimizationOptions = OptimizationOptions.PRETTY,
        executor: Executor = defaultThreadPool(),
        processStatusCallBack: ProcessCallBack = SysOutProcessStatusCallBack(),
        allowErrors: Boolean = true,
    ): this(
        optimizationOptions = optimizationOptions,
        coroutineDispatcher = executor.asCoroutineDispatcher(),
        callback = processStatusCallBack,
        allowErrors = allowErrors,
    )

    @JvmField
    val classes: LinkedHashMap<String, ByteArray> = LinkedHashMap()

    @JvmField
    val errors: LinkedHashMap<String, String> = LinkedHashMap()

    @JvmField
    val warnings: LinkedHashMap<String, String> = LinkedHashMap()

    private var totalClassCount = 0

    private val resultsMutex: Mutex = Mutex()

    private val jvmClassWriter: JVMClassWriter = JVMClassWriter()

    @Suppress("unused")
    fun process(dexFileDataList: List<ByteArray>) = runBlocking {
        suspendProcess(dexFileDataList)
    }

    suspend fun suspendProcess(dexFileDataList: List<ByteArray>) {
        val dexFiles = dexFileDataList.map {
            DexFile(it)
        }

        totalClassCount = dexFiles.sumOf { it.classes.size }

        callback.onProgress(
            translated = 0,
            warnings = 0,
            errors = 0,
            total = totalClassCount,
        )

        val coroutineDispatcher = coroutineDispatcher
        dexFiles.suspendMapTo(
            ArrayList<Unit>(),
            coroutineDispatcher
        ) { dex ->
            dex.classes.suspendMapTo(
                target = ArrayList(),
                dispatcher = coroutineDispatcher,
                f = ::translateClass
            )
        }

        callback.onProgress(
            translated = classes.size,
            warnings = errors.size,
            errors = warnings.size,
            total = totalClassCount
        )
    }

    suspend fun translateClass(dexClass: DexClass) {
        val unicodeName = decode(dexClass.name) + ".class"

        resultsMutex.withLock {
            if (classes.containsKey(unicodeName) || errors.containsKey(unicodeName)) {
                addWarning(unicodeName, "Duplicate class name $unicodeName")
                return
            }
        }

        try {
            val classData: ByteArray = jvmClassWriter.toClassFile(
                cls = dexClass,
                opts = optimizationOptions,
            )

            addSuccess(unicodeName, classData)
        } catch (e: Exception) {
            if (!allowErrors) {
                throw e
            }

            addError(unicodeName, e.message ?: "Unknown error")
        }
    }

    internal suspend fun addWarning(className: String, warning: String) {
        resultsMutex.withLock {
            warnings.put(className, warning)
            callOnProgress()
        }
    }

    internal suspend fun addError(className: String, error: String) {
        resultsMutex.withLock {
            errors.put(className, error)
            callOnProgress()
        }
    }

    internal suspend fun addSuccess(className: String, result: ByteArray) {
        resultsMutex.withLock {
            classes.put(className, result)
            callOnProgress()
        }

        callback.suspendOnClassTranslated(className, result)
    }

    private fun callOnProgress() {
        val warnings = warnings.size
        val errors = errors.size
        val processed = classes.size + warnings + errors
        if (processed % 1000 == 0) {
            callback.onProgress(
                translated = processed,
                warnings = warnings,
                errors = errors,
                total = totalClassCount
            )
        }
    }

    interface ProcessCallBack {
        fun onProgress(translated: Int, warnings: Int, errors: Int, total: Int) {}

        suspend fun suspendOnClassTranslated(unicodeName: String, classData: ByteArray) {
            onClassTranslated(unicodeName, classData)
        }

        fun onClassTranslated(unicodeName: String, classData: ByteArray) {}
    }

    open class SysOutProcessStatusCallBack: ProcessCallBack {

        override fun onProgress(translated: Int, warnings: Int, errors: Int, total: Int) {
            if (translated + errors < total) {
                println("Processing... $translated classes processed, error count: $errors, total: $total")
            } else {
                println("$translated classes translated successfully, $errors classes had errors.")
            }
        }
    }
}