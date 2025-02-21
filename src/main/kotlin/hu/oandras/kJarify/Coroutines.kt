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

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

internal suspend fun <A, B, ListType : MutableList<B>> List<A>.suspendMapTo(
    target: ListType,
    dispatcher: CoroutineContext,
    f: suspend (A) -> B
): ListType {
    suspendMapToImpl(target, dispatcher, f)
    return target
}

private suspend fun <A, B, ListType : MutableList<B>> List<A>.suspendMapToImpl(
    target: ListType?,
    dispatcher: CoroutineContext,
    f: suspend (A) -> B
) {
    return coroutineScope {
        Array(size) {
            async(dispatcher) { f(get(it)) }
        }.forEach {
            val deferred = it.await()
            target?.add(deferred)
        }
    }
}

internal fun defaultThreadPool(): Executor {
    return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)
}