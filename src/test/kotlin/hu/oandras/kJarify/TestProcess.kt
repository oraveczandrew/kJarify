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

import hu.oandras.kJarify.streams.decode
import hu.oandras.kJarify.dex.DexFile
import hu.oandras.kJarify.jvm.JVMClassWriter
import hu.oandras.kJarify.jvm.optimization.OptimizationOptions
import hu.oandras.kJarify.streams.readAndClose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TestProcess {

    // Test 6 and 7 has invalid examples, so they not pass
    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5, /*6, 7*/])
    fun test1(n: Int) {
        val dexData = this::class.java.getResourceAsStream("/test$n/classes.dex")!!.readAndClose()

        val dex = DexFile(dexData)

        val byteCodes = HashMap<String, ByteArray>()

        val jvmClassWriter = JVMClassWriter()

        dex.classes.forEach {
            val decodedName = decode(it.name)

            byteCodes.put(
                decodedName.replace("/", "."),
                jvmClassWriter.toClassFile(it, OptimizationOptions.PRETTY)
            )
        }

        val classLoader = object : ClassLoader() {
            override fun findClass(name: String): Class<*> {
                val bytecode: ByteArray? = byteCodes[name]
                return if (bytecode != null) {
                    defineClass(name, bytecode, 0, bytecode.size)
                } else {
                    stubClassLoder.loadClass(name)
                }
            }
        }

        val givenOutput = withCapturedOutput {
            val bundleClazz = classLoader.loadClass("android.os.Bundle")
            val clazz = classLoader.loadClass("a.a")
            @Suppress("DEPRECATION")
            val instance = clazz.newInstance()
            val oncCreateMethod = clazz.getDeclaredMethod("onCreate", bundleClazz)
            oncCreateMethod.invoke(instance, null)
        }

        assertOutput("src/test/resources/test$n/expected.txt", givenOutput)
    }

    private fun assertOutput(
        expectedTextFilePath: String,
        givenOutput: List<String?>,
    ) {
        val file = File(expectedTextFilePath)
        val expectedText = file.readText()

        val lines = expectedText.split("\r\n").map { it.trim { it < ' ' } }

        assertEquals(lines.size, givenOutput.size)

        for (i in lines.indices) {
            assertEquals(lines[i], givenOutput[i])
        }
    }

    companion object {

        private val stubClassLoder: ClassLoader = kotlin.run {
            val subs = File("src/test/resources/stubs/stubs.zip")
            assertTrue(subs.exists())
            ZipClassLoader(subs)
        }
    }
}
