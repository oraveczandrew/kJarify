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

package hu.oandras.kJarify.jvm

import hu.oandras.kJarify.*
import hu.oandras.kJarify.dex.*
import hu.oandras.kJarify.jvm.constants.Calculator
import hu.oandras.kJarify.jvm.optimization.*
import hu.oandras.kJarify.streams.Writer
import hu.oandras.kJarify.streams.withWriter

class JVMClassWriter {

    private val calculator = Calculator()

    @Throws(ClassFileLimitExceeded::class)
    fun toClassFile(
        cls: DexClass,
        opts: OptimizationOptions
    ): ByteArray {
        // Optimistically try translating without optimization to speed things up
        // if the resulting code is too big, retry with optimization
        return try {
            writeWithOptimizationOptions(
                cls = cls,
                opts = opts
            )
        } catch (_: ClassFileLimitExceeded) {
            println("Retrying " + cls.name + " with all optimization enabled")

            writeWithOptimizationOptions(
                cls = cls,
                opts = OptimizationOptions.ALL
            )
        }
    }

    
    private fun writeWithOptimizationOptions(
        cls: DexClass,
        opts: OptimizationOptions,
    ): ByteArray {
        val params = IRWriter.Parameters(
            calculator = calculator,
            pool = if (opts.splitPool) SplitConstantPool() else SimpleConstantPool(),
            optimizationOptions = opts,
        )

        val result = classFileAfterPool(cls, params)

        return withWriter { stream ->
            stream.u32(-0x35014542)
            // bytecode version 49.0
            stream.u16(0)
            stream.u16(49)

            // write constant pool
            result.first.write(stream)
            // write rest of file
            stream.write(result.second)
        }
    }

    
    @Throws(ClassFileLimitExceeded::class)
    private fun classFileAfterPool(
        cls: DexClass,
        params: IRWriter.Parameters,
    ): Pair<ConstantPool, ByteArray> {
        val pool = params.pool

        cls.parseData()

        var access = cls.access and Flags.CLASS_FLAGS
        if (access and Flags.ACC_INTERFACE == 0) {
            // Not necessary for correctness, but this works around a bug in dx
            access = access or Flags.ACC_SUPER
        }

        val bytes = withWriter { stream ->
            stream.u16(access) // access
            stream.u16(pool.classRef(cls.name)) // this

            val superClass = cls.superClass
            val superClassRef = if (superClass != null) {
                pool.classRef(superClass)
            } else {
                0
            }
            stream.u16(superClassRef) // super

            // interfaces
            val interfaces = cls.interfaces
            stream.u16(interfaces.size)
            for (interfaceName in interfaces) {
                stream.u16(pool.classRef(interfaceName))
            }

            val data = cls.data!!

            writeFields(pool, stream, data.fields)

            writeMethods(params, stream, data.methods)

            // attributes
            stream.u16(0)
        }

        return Pair(pool, bytes)
    }

    
    @Throws(ClassFileLimitExceeded::class)
    private fun writeMethods(params: IRWriter.Parameters, stream: Writer, methods: List<Method>) {
        val codeIRs: ArrayList<IRWriter> = ArrayList(methods.size)

        methods.forEachElement { method ->
            if (method.code != null) {
                codeIRs.add(getCodeIR(params, method))
            }
        }

        val codeAttrsBytes = BytecodeAssembler.finishCodeAttrs(
            pool = params.pool,
            codeIRs = codeIRs,
            opts = params.optimizationOptions
        )

        stream.u16(methods.size)

        methods.forEachElement { method ->
            writeMethod(
                pool = params.pool,
                stream = stream,
                method = method,
                codeAttrBytes = codeAttrsBytes[method]
            )
        }
    }

    
    private fun getCodeIR(params: IRWriter.Parameters, method: Method): IRWriter {
        val irData = IRWriter.writeBytecode(
            params = params,
            method = method,
        )

        runOptimizations(irData, params.optimizationOptions)

        return irData
    }

    private fun runOptimizations(irData: IRWriter, options: OptimizationOptions) {
        if (options.inlineConsts) {
            InlineConstsOptimization.optimize(irData)
        }

        if (options.copyPropagation) {
            CopyPropagationOptimization.optimize(irData)
        }

        if (options.removeUnusedRegs) {
            UnusedRegisterRemovalOptimization.optimize(irData)
        }

        if (options.dup2ize) {
            Dup2izeOptimization.optimize(irData)
        }

        if (options.pruneStoreLoads) {
            StoreLoadPruneOptimization.optimize(irData)
            if (options.removeUnusedRegs) {
                UnusedRegisterRemovalOptimization.optimize(irData)
            }
        }

        val customOpts = options.customOptimizations
        customOpts.forEachElement {
            it.optimize(irData)
        }

        if (options.sortRegisters) {
            Registers.sortAllocateRegisters(irData)
        } else {
            Registers.simpleAllocateRegisters(irData)
        }
    }

    
    @Throws(ClassFileLimitExceeded::class)
    private fun writeMethod(pool: ConstantPool, stream: Writer, method: Method, codeAttrBytes: ByteArray?) {
        val methodId = method.id

        stream.u16(method.access and Flags.METHOD_FLAGS)
        stream.u16(pool.utf8Ref(methodId.name))
        stream.u16(pool.utf8Ref(methodId.descriptor))

        if (codeAttrBytes != null) {
            stream.u16(1)
            stream.u16(pool.utf8Ref(CODE))
            stream.u32(codeAttrBytes.size)
            stream.write(codeAttrBytes)
        } else {
            stream.u16(0) // no attributes
        }
    }

    
    private fun writeFields(pool: ConstantPool, stream: Writer, fields: List<Field>) {
        stream.u16(fields.size)
        fields.forEachElement { field ->
            writeField(pool, stream, field)
        }
    }

    
    @Suppress("DEPRECATION")
    @Throws(ClassFileLimitExceeded::class)
    private fun writeField(pool: ConstantPool, stream: Writer, field: Field) {
        stream.u16(field.access and Flags.FIELD_FLAGS)
        stream.u16(pool.utf8Ref(field.id.name))

        val descriptor = field.id.descriptor

        stream.u16(pool.utf8Ref(descriptor))

        val classTypeVal = field.constantValue
        if (classTypeVal != null) {
            stream.u16(1)
            stream.u16(pool.utf8Ref(ConstantValueBytes))
            stream.u32(2)

            // Ignore dalvik constant type and use actual field type instead
            val index = when (descriptor.size) {
                1 -> {
                    when (descriptor[0].toInt()) {
                        'Z'.toInt(),
                        'B'.toInt(),
                        'S'.toInt(),
                        'C'.toInt(),
                        'I'.toInt() -> pool.intRef((classTypeVal as ConstantValue.IntConstant).value)
                        'F'.toInt() -> pool.floatRef((classTypeVal as ConstantValue.IntConstant).value)
                        'J'.toInt() -> pool.longRef((classTypeVal as ConstantValue.LongConstant).value)
                        'D'.toInt() -> pool.doubleRef((classTypeVal as ConstantValue.LongConstant).value)
                        else -> error("Unknown descriptor: $descriptor")
                    }
                }
                else -> {
                    when {
                        descriptor.contentEquals(JAVA_LANG_STRING_BYTES) -> {
                            pool.stringRef((classTypeVal as ConstantValue.ByteArrayConstant).value)
                        }
                        descriptor.contentEquals(JAVA_LANG_CLASS_BYTES) -> {
                            pool.classRef((classTypeVal as ConstantValue.ByteArrayConstant).value)
                        }
                        else -> {
                            error("Unknown descriptor: $descriptor")
                        }
                    }
                }
            }
            stream.u16(index)
        } else {
            stream.u16(0) // no attributes
        }
    }
}

