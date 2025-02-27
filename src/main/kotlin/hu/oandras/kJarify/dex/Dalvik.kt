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

@file:OptIn(ExperimentalStdlibApi::class)

package hu.oandras.kJarify.dex

import androidx.collection.*
import hu.oandras.kJarify.L_JAVA_LANG_OBJECT
import hu.oandras.kJarify.equalsWithChar
import hu.oandras.kJarify.keysToRanges
import hu.oandras.kJarify.streams.Stream

@JvmField
val INVOKE_TYPES: Array<DalvikOpcode> = arrayOf(
    DalvikOpcode.InvokeVirtual,
    DalvikOpcode.InvokeSuper,
    DalvikOpcode.InvokeDirect,
    DalvikOpcode.InvokeStatic,
    DalvikOpcode.InvokeInterface
)

@Suppress("unused")
@JvmField
val THROW_TYPES: Set<DalvikOpcode> = arraySetOf(
    DalvikOpcode.InvokeVirtual,
    DalvikOpcode.InvokeSuper,
    DalvikOpcode.InvokeDirect,
    DalvikOpcode.InvokeStatic,
    DalvikOpcode.InvokeInterface,
    DalvikOpcode.ConstString,
    DalvikOpcode.ConstClass,
    DalvikOpcode.MonitorEnter,
    DalvikOpcode.MonitorExit,
    DalvikOpcode.CheckCast,
    DalvikOpcode.InstanceOf,
    DalvikOpcode.ArrayLen,
    DalvikOpcode.NewArray,
    DalvikOpcode.NewInstance,
    DalvikOpcode.FilledNewArray,
    DalvikOpcode.FillArrayData,
    DalvikOpcode.Throw,
    DalvikOpcode.ArrayGet,
    DalvikOpcode.ArrayPut,
    DalvikOpcode.InstanceGet,
    DalvikOpcode.InstancePut,
    DalvikOpcode.StaticGet,
    DalvikOpcode.StaticPut,
    DalvikOpcode.BinaryOp,
    DalvikOpcode.BinaryOpConst
)

@JvmField
val PRUNED_THROW_TYPES: Set<DalvikOpcode> = arraySetOf(
    DalvikOpcode.InvokeVirtual,
    DalvikOpcode.InvokeSuper,
    DalvikOpcode.InvokeDirect,
    DalvikOpcode.InvokeStatic,
    DalvikOpcode.InvokeInterface,
    DalvikOpcode.MonitorEnter,
    DalvikOpcode.MonitorExit,
    DalvikOpcode.CheckCast,
    DalvikOpcode.ArrayLen,
    DalvikOpcode.NewArray,
    DalvikOpcode.NewInstance,
    DalvikOpcode.FilledNewArray,
    DalvikOpcode.FillArrayData,
    DalvikOpcode.Throw,
    DalvikOpcode.ArrayGet,
    DalvikOpcode.ArrayPut,
    DalvikOpcode.InstanceGet,
    DalvikOpcode.InstancePut,
    DalvikOpcode.StaticGet,
    DalvikOpcode.StaticPut,
    DalvikOpcode.BinaryOp,
    DalvikOpcode.BinaryOpConst
)

private val OPCODES: IntObjectMap<DalvikOpcode> = keysToRanges(MutableIntObjectMap<DalvikOpcode>().apply {
    put(0x00, DalvikOpcode.Nop)
    put(0x01, DalvikOpcode.Move)
    put(0x04, DalvikOpcode.MoveWide)
    put(0x07, DalvikOpcode.Move)
    put(0x0a, DalvikOpcode.MoveResult)
    put(0x0e, DalvikOpcode.Return)
    put(0x12, DalvikOpcode.Const32)
    put(0x16, DalvikOpcode.Const64)
    put(0x1a, DalvikOpcode.ConstString)
    put(0x1c, DalvikOpcode.ConstClass)
    put(0x1d, DalvikOpcode.MonitorEnter)
    put(0x1e, DalvikOpcode.MonitorExit)
    put(0x1f, DalvikOpcode.CheckCast)
    put(0x20, DalvikOpcode.InstanceOf)
    put(0x21, DalvikOpcode.ArrayLen)
    put(0x22, DalvikOpcode.NewInstance)
    put(0x23, DalvikOpcode.NewArray)
    put(0x24, DalvikOpcode.FilledNewArray)
    put(0x26, DalvikOpcode.FillArrayData)
    put(0x27, DalvikOpcode.Throw)
    put(0x28, DalvikOpcode.Goto)
    put(0x2b, DalvikOpcode.Switch)
    put(0x2d, DalvikOpcode.Cmp)
    put(0x32, DalvikOpcode.If)
    put(0x38, DalvikOpcode.IfZ)
    put(0x3e, DalvikOpcode.Nop) // unused
    put(0x44, DalvikOpcode.ArrayGet)
    put(0x4b, DalvikOpcode.ArrayPut)
    put(0x52, DalvikOpcode.InstanceGet)
    put(0x59, DalvikOpcode.InstancePut)
    put(0x60, DalvikOpcode.StaticGet)
    put(0x67, DalvikOpcode.StaticPut)
    put(0x6e, DalvikOpcode.InvokeVirtual)
    put(0x6f, DalvikOpcode.InvokeSuper)
    put(0x70, DalvikOpcode.InvokeDirect)
    put(0x71, DalvikOpcode.InvokeStatic)
    put(0x72, DalvikOpcode.InvokeInterface)
    put(0x73, DalvikOpcode.Nop) // unused
    put(0x74, DalvikOpcode.InvokeVirtual)
    put(0x75, DalvikOpcode.InvokeSuper)
    put(0x76, DalvikOpcode.InvokeDirect)
    put(0x77, DalvikOpcode.InvokeStatic)
    put(0x78, DalvikOpcode.InvokeInterface)
    put(0x79, DalvikOpcode.Nop) // unused
    put(0x7b, DalvikOpcode.UnaryOp)
    put(0x90, DalvikOpcode.BinaryOp)
    put(0xd0, DalvikOpcode.BinaryOpConst)
    put(0xe3, DalvikOpcode.Nop) // unused
}, 256)

fun parseInstruction(
    dex: DexFile,
    instructionStartPosition: Int,
    shorts: IntArray,
    position: Int
): DalvikInstruction {
    val word = shorts[position]
    val opcode = word and 0xFF
    val result = InstructionParameterDecoder.decode(shorts, position, opcode)
    var newPos = result.pos
    val args = result.args

    // parse special data instructions
    var switchData: IntIntMap? = null
    var fillArrData: ArrayData? = null

    if (word == 0x100 || word == 0x200) {
        // switch
        val size = shorts[position + 1]
        val st: Stream = dex.stream(instructionStartPosition + position * 2 + 4)

        if (word == 0x100) {
            // packed
            val firstKey = st.u32()
            val targets = IntArray(size) {
                st.u32()
            }
            newPos = position + 2 + (1 + size) * 2
            switchData = MutableIntIntMap(targets.size)
            for (i in targets.indices) {
                switchData.put(i + firstKey, targets[i])
            }
        } else {
            // sparse
            val keys = IntArray(size) {
                st.u32()
            }

            val targets = IntArray(size) {
                st.u32()
            }

            newPos = position + 2 + (size + size) * 2
            switchData = MutableIntIntMap(targets.size)
            for (i in keys.indices) {
                switchData.put(keys[i], targets[i])
            }
        }
    }

    if (word == 0x300) {
        val width = shorts[position + 1] % 16
        val size = shorts[position + 2] xor (shorts[position + 3] shl 16)
        newPos = position + Math.floorDiv(size * width + 1, 2) + 4
        // get array data
        val stream: Stream = dex.stream(instructionStartPosition + position * 2 + 8)
        fillArrData = when (width) {
            1 -> ArrayData.ArrayData32(
                width,
                IntArray(size) {
                    stream.u8()
                }
            )
            2 -> ArrayData.ArrayData32(
                width,
                IntArray(size) {
                    stream.u16()
                }
            )
            4 -> ArrayData.ArrayData32(
                width,
                IntArray(size) {
                    stream.u32()
                }
            )
            8 -> ArrayData.ArrayData64(
                width,
                LongArray(size) {
                    stream.u64()
                }
            )
            else -> throw IllegalArgumentException("Invalid width")
        }
    }

    // warning, this must go below the special data handling that calculates newPos
    val instruction = DalvikInstruction(
        type = OPCODES[opcode] ?: throw RuntimeException("Invalid opcode: 0x${opcode.toHexString()}"),
        position = position,
        pos2 = newPos,
        opcode = opcode,
        args = args,
        switchData = switchData,
        fillArrData = fillArrData
    )

    return instruction
}

fun parseBytecode(
    dex: DexFile,
    instructionStartPosition: Int,
    shorts: IntArray,
    catchAddresses: IntSet
): Array<DalvikInstruction> {
    val ops: ArrayList<DalvikInstruction> = ArrayList(shorts.size / 3)
    var pos = 0
    while (pos < shorts.size) {
        val result = parseInstruction(
            dex = dex,
            instructionStartPosition = instructionStartPosition,
            shorts = shorts,
            position = pos
        )
        pos = result.pos2
        ops.add(result)
    }

    // Fill in data for move-result
    for (i in 0 until ops.size - 1) {
        val instr2 = ops[i + 1]

        if (instr2.type != DalvikOpcode.MoveResult) {
            continue
        }

        val instr = ops[i]

        if (INVOKE_TYPES.contains(instr.type)) {
            val calledId: MethodId = dex.methodId(instr.getIntArg(0))
            val returnType = calledId.returnType
            if (!returnType.equalsWithChar('V')) {
                instr2.prevResult = returnType
            }
        } else if (instr.type == DalvikOpcode.FilledNewArray) {
            instr2.prevResult = dex.type(instr.getIntArg(0))
        } else if (catchAddresses.contains(instr2.position)) {
            instr2.prevResult = L_JAVA_LANG_OBJECT
        }
    }

    assert(!catchAddresses.contains(0))

    // Fill in implicit cast data
    for (i in 1 until ops.size) {
        val instr = ops[i]
        if (instr.opcode == 0x38 || instr.opcode == 0x39) { // if-eqz, if-nez
            if (ops[i - 1].type == DalvikOpcode.InstanceOf) {
                val prev = ops[i - 1]
                val descriptorIndex: Int = prev.getIntArg(2)
                val registries = MutableIntSet()
                registries.add(prev.getIntArg(1))

                if (i > 1 && ops[i - 2].type == DalvikOpcode.Move) {
                    val prev2 = ops[i - 2]
                    if (prev2.getIntArg(0) == prev.getIntArg(1)) {
                        registries.add(prev2.getIntArg(1))
                    }
                }

                // Don't cast result of instanceof if it overwrites the input
                registries.remove(prev.getIntArg(0))
                if (registries.isNotEmpty()) {
                    instr.implicitCasts = ImplicitCasts(
                        descriptorIndex = descriptorIndex,
                        registries = registries
                    )
                }
            }
        }
    }

    return ops.toTypedArray()
}
