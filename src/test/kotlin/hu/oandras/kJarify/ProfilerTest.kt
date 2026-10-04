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
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.jupiter.api.Test

import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicLong

class ProfilerTest {

    @Test
    fun profileAppBeta() {
        // -Dprofiler.input=test2-base.apk -Dprofiler.runs=2 -Dprofiler.warmup=1
        val input = File(
            System.getProperty(
                "profiler.input",
                "src/test/resources/ksvg-showcase-1.0.0-beta01.apk",
            )
        )
        require(input.exists()) { "Missing input: ${input.absolutePath}" }
        val runs = System.getProperty("profiler.runs", "3").toInt()
        val warmups = System.getProperty("profiler.warmup", "1").toInt()
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

        // Warmup (JIT): full translations, results discarded
        println("--- $warmups warmup run(s) ---")
        gc()
        repeat(warmups) {
            val w0 = System.nanoTime()
            val (wn, _) = runBlockingTranslate(dexDataList)
            val w1 = System.nanoTime()
            println("warmup: ${(w1 - w0) / 1_000_000} ms, classes=$wn")
        }

        // Measured runs
        val times = mutableListOf<Long>()
        var lastClasses = 0
        var lastBytes = 0L
        val threadMx = ManagementFactory.getThreadMXBean()
        repeat(runs) { i ->
            gc()
            val memBefore = usedMb()
            val g0 = gcCount(); val gt0 = gcTime()
            val cpu0 = totalCpuTime(threadMx)
            val t0 = System.nanoTime()
            val (n, bytes) = runBlockingTranslate(dexDataList)
            val t1 = System.nanoTime()
            val cpu1 = totalCpuTime(threadMx)
            val g1 = gcCount(); val gt1 = gcTime()
            lastClasses = n; lastBytes = bytes
            val ms = (t1 - t0) / 1_000_000
            times.add(ms)
            val memAfter = usedMb()
            val effCores = (cpu1 - cpu0).toDouble() / (t1 - t0).coerceAtLeast(1)
            println("run ${i + 1}/$runs: $ms ms, classes=$n, out=${"%.2f".format(bytes / 1024.0 / 1024.0)} MB, " +
                "heap ${"%.1f".format(memBefore)} -> ${"%.1f".format(memAfter)} MB, " +
                "GC pauses=${g1 - g0}, GC time=${gt1 - gt0} ms, " +
                "eff.cores=${"%.1f".format(effCores)}")
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
                appendLine("input=${input.name} (${"%.2f".format(apkSizeMb)} MB)")
                appendLine("dexFiles=${dexDataList.size} dexTotalMb=${"%.2f".format(dexTotalMb)}")
                appendLine("classes=$classCount")
                appendLine("timesMs=$times avgMs=${"%.0f".format(avg)} bestMs=$best")
            })
        }
    }

    private fun totalCpuTime(threadMx: java.lang.management.ThreadMXBean): Long {
        if (!threadMx.isThreadCpuTimeSupported) return 0L
        return threadMx.allThreadIds.sumOf { id ->
            val t = threadMx.getThreadCpuTime(id).coerceAtLeast(0L)
            t
        }
    }

    private fun runBlockingTranslate(dexDataList: List<ByteArray>): Pair<Int, Long> {
        val outBytes = AtomicLong(0)
        val cb = object : DexProcessor.ProcessCallBack() {
            override fun onClassTranslated(unicodeRelativePath: String, classData: ByteArray) {
                outBytes.addAndGet(classData.size.toLong())
            }
        }
        // -Dprofiler.threads=N overrides worker count (default: Dispatchers.Default)
        val threads = System.getProperty("profiler.threads")?.toIntOrNull()
        val dispatcher = if (threads != null) {
            java.util.concurrent.Executors.newFixedThreadPool(threads).asCoroutineDispatcher()
        } else {
            Dispatchers.Default
        }
        try {
            val proc = DexProcessor(
                optimizationOptions = OptimizationOptions.PRETTY,
                coroutineDispatcher = dispatcher,
                callback = cb,
            )
            kotlinx.coroutines.runBlocking { proc.suspendProcess(dexDataList) }
            return proc.classes.size to outBytes.get()
        } finally {
            if (dispatcher !== Dispatchers.Default) {
                (dispatcher as java.io.Closeable).close()
            }
        }
    }
}
