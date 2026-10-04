/*
 * Copyright (C) 2026 András Oravecz and the contributors.
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

import hu.oandras.kJarify.dex.DexFile
import hu.oandras.kJarify.dex.DexProcessor
import hu.oandras.kJarify.dex.DexReader
import hu.oandras.kJarify.jvm.optimization.OptimizationOptions
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicLong

class ProfilerTest {

    @Test
    fun profileAppBeta() {
        val input = File("app-beta.apk")
        require(input.exists()) { "Missing input: ${input.absolutePath}" }
        val apkSizeMb = input.length() / 1024.0 / 1024.0
        println("=== kJarify profiler ===")
        println("Input: ${input.absolutePath} (${"%.2f".format(apkSizeMb)} MB)")
        println("CPUs: ${Runtime.getRuntime().availableProcessors()}, JVM: ${System.getProperty("java.version")} ${System.getProperty("java.vm.name")}")
        println("Optimization: PRETTY")

        // Phase 0: DEX extraction (IO) - once, reused
        val tRead0 = System.nanoTime()
        val dexDataList = DexReader.ApkDexFileReader.read(input.absolutePath)
        val tRead1 = System.nanoTime()
        val dexTotalMb = dexDataList.sumOf { it.size } / 1024.0 / 1024.0
        println("DEX files: ${dexDataList.size}, total DEX size: ${"%.2f".format(dexTotalMb)} MB, read in ${(tRead1 - tRead0) / 1_000_000} ms")

        // Phase 1: DEX parsing (DexFile ctor) measured separately
        val tParse0 = System.nanoTime()
        val dexFiles = dexDataList.map { DexFile(it) }
        val tParse1 = System.nanoTime()
        val classCount = dexFiles.sumOf { it.classes.size }
        println("Classes: $classCount, parse time: ${(tParse1 - tParse0) / 1_000_000} ms")

        fun gc() { repeat(2) { System.gc(); Thread.sleep(100) } }
        fun usedMb() = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0 / 1024.0

        val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()
        fun gcCount() = gcBeans.sumOf { it.collectionCount }
        fun gcTime() = gcBeans.sumOf { it.collectionTime }

        // Warmup (JIT): 1 full translation, result discarded
        println("--- warmup run ---")
        gc()
        val w0 = System.nanoTime()
        runBlockingTranslate(dexDataList)
        val w1 = System.nanoTime()
        println("warmup: ${(w1 - w0) / 1_000_000} ms")

        // Measured runs
        val runs = 3
        val times = mutableListOf<Long>()
        var lastClasses = 0
        var lastBytes = 0L
        repeat(runs) { i ->
            gc()
            val memBefore = usedMb()
            val g0 = gcCount(); val gt0 = gcTime()
            val t0 = System.nanoTime()
            val (n, bytes) = runBlockingTranslate(dexDataList)
            val t1 = System.nanoTime()
            val g1 = gcCount(); val gt1 = gcTime()
            lastClasses = n; lastBytes = bytes
            val ms = (t1 - t0) / 1_000_000
            times.add(ms)
            val memAfter = usedMb()
            println("run ${i + 1}/$runs: $ms ms, classes=$n, out=${"%.2f".format(bytes / 1024.0 / 1024.0)} MB, " +
                "heap ${"%.1f".format(memBefore)} -> ${"%.1f".format(memAfter)} MB, " +
                "GC pauses=${g1 - g0}, GC time=${gt1 - gt0} ms")
        }

        val avg = times.average()
        val best = times.min()
        println("=== summary ===")
        println("avg: ${"%.0f".format(avg)} ms, best: $best ms, worst: ${times.max()} ms")
        println("throughput (best): ${"%.2f".format(dexTotalMb / (best / 1000.0))} MB DEX/s, " +
            "${"%.0f".format(classCount.toDouble() / (best / 1000.0))} classes/s")
        println("output: $lastClasses classes, ${"%.2f".format(lastBytes / 1024.0 / 1024.0)} MB bytecode")
        println("peak threads: ${ManagementFactory.getThreadMXBean().threadCount} live")

        // Top heap consumers hint
        println("heap max: ${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB")
        File("build/profiler-report.txt").apply {
            parentFile.mkdirs()
            writeText(buildString {
                appendLine("input=app-beta.apk (${"%.2f".format(apkSizeMb)} MB)")
                appendLine("dexFiles=${dexDataList.size} dexTotalMb=${"%.2f".format(dexTotalMb)}")
                appendLine("classes=$classCount")
                appendLine("timesMs=$times avgMs=${"%.0f".format(avg)} bestMs=$best")
            })
        }
    }

    private fun runBlockingTranslate(dexDataList: List<ByteArray>): Pair<Int, Long> {
        val outBytes = AtomicLong(0)
        val cb = object : DexProcessor.ProcessCallBack() {
            override fun onClassTranslated(unicodeRelativePath: String, classData: ByteArray) {
                outBytes.addAndGet(classData.size.toLong())
            }
        }
        val proc = DexProcessor(
            optimizationOptions = OptimizationOptions.PRETTY,
            coroutineDispatcher = Dispatchers.Default,
            callback = cb,
        )
        kotlinx.coroutines.runBlocking { proc.suspendProcess(dexDataList) }
        return proc.classes.size to outBytes.get()
    }
}
