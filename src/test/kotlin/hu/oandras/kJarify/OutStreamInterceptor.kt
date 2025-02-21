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

import java.io.OutputStream
import java.io.PrintStream

private class OutStreamInterceptor(
    outputStream: OutputStream,
): PrintStream(outputStream, true) {

    @JvmField
    val capturedData: ArrayList<String?> = ArrayList()

    override fun println(s: String?) {
        capturedData.add(s)
    }
}

fun withCapturedOutput(r: () -> Unit): List<String?> {
    val origOut = System.out

    return try {
        val capturer = OutStreamInterceptor(origOut)
        System.setOut(capturer)
        r.invoke()
        capturer.capturedData
    } finally {
        System.setOut(origOut)
    }
}