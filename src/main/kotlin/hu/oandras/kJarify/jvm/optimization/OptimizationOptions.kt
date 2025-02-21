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

package hu.oandras.kJarify.jvm.optimization

data class OptimizationOptions(
    @JvmField
    val inlineConsts: Boolean,
    @JvmField
    val pruneStoreLoads: Boolean,
    @JvmField
    val copyPropagation: Boolean,
    @JvmField
    val removeUnusedRegs: Boolean,
    @JvmField
    val dup2ize: Boolean,
    @JvmField
    val sortRegisters: Boolean,
    @JvmField
    val splitPool: Boolean,
    @JvmField
    val delayConsts: Boolean,
    @JvmField
    val customOptimizations: List<JvmOptimization>,
) {
    companion object {
        @JvmField
        val NONE: OptimizationOptions = OptimizationOptions(
            inlineConsts = false,
            pruneStoreLoads = false,
            copyPropagation = false,
            removeUnusedRegs = false,
            dup2ize = false,
            sortRegisters = false,
            splitPool = false,
            delayConsts = false,
            customOptimizations = emptyList(),
        )

        // Options which make the generated code more readable for humans
        @JvmField
        val PRETTY: OptimizationOptions = OptimizationOptions(
            inlineConsts = true,
            pruneStoreLoads = true,
            copyPropagation = true,
            removeUnusedRegs = true,
            dup2ize = false,
            sortRegisters = false,
            splitPool = false,
            delayConsts = false,
            customOptimizations = emptyList(),
        )

        @JvmField
        val ALL: OptimizationOptions = OptimizationOptions(
            inlineConsts = true,
            pruneStoreLoads = true,
            copyPropagation = true,
            removeUnusedRegs = true,
            dup2ize = true,
            sortRegisters = true,
            splitPool = true,
            delayConsts = true,
            customOptimizations = emptyList(),
        )
    }
}
