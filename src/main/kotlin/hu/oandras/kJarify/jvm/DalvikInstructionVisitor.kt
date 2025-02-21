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

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntIntMap
import hu.oandras.kJarify.JAVA_LANG_THROWABLE
import hu.oandras.kJarify.byteArrayOf_u8
import hu.oandras.kJarify.byteArrayOf_u8u16
import hu.oandras.kJarify.dex.*
import hu.oandras.kJarify.dex.DalvikOpcode.*
import hu.oandras.kJarify.equalsWithChar
import hu.oandras.kJarify.jvm.ArrayOpByteCodeLookUp.ByteArrayKey
import hu.oandras.kJarify.jvm.ArrayOpByteCodeLookUp.arrLoadOps
import hu.oandras.kJarify.jvm.ArrayOpByteCodeLookUp.arrStoreOps
import hu.oandras.kJarify.jvm.ArrayTypes.eletPair
import hu.oandras.kJarify.jvm.JvmInstruction.*
import hu.oandras.kJarify.jvm.JvmOps.AALOAD
import hu.oandras.kJarify.jvm.JvmOps.AASTORE
import hu.oandras.kJarify.jvm.JvmOps.ARRAYLENGTH
import hu.oandras.kJarify.jvm.JvmOps.ATHROW
import hu.oandras.kJarify.jvm.JvmOps.DCMPG
import hu.oandras.kJarify.jvm.JvmOps.DCMPL
import hu.oandras.kJarify.jvm.JvmOps.FCMPG
import hu.oandras.kJarify.jvm.JvmOps.FCMPL
import hu.oandras.kJarify.jvm.JvmOps.GETFIELD
import hu.oandras.kJarify.jvm.JvmOps.GETSTATIC
import hu.oandras.kJarify.jvm.JvmOps.I2L
import hu.oandras.kJarify.jvm.JvmOps.ICONST_M1
import hu.oandras.kJarify.jvm.JvmOps.IFEQ
import hu.oandras.kJarify.jvm.JvmOps.IFGE
import hu.oandras.kJarify.jvm.JvmOps.IFGT
import hu.oandras.kJarify.jvm.JvmOps.IFLE
import hu.oandras.kJarify.jvm.JvmOps.IFLT
import hu.oandras.kJarify.jvm.JvmOps.IFNE
import hu.oandras.kJarify.jvm.JvmOps.IFNONNULL
import hu.oandras.kJarify.jvm.JvmOps.IFNULL
import hu.oandras.kJarify.jvm.JvmOps.IF_ACMPEQ
import hu.oandras.kJarify.jvm.JvmOps.IF_ACMPNE
import hu.oandras.kJarify.jvm.JvmOps.IF_ICMPEQ
import hu.oandras.kJarify.jvm.JvmOps.IF_ICMPGE
import hu.oandras.kJarify.jvm.JvmOps.IF_ICMPGT
import hu.oandras.kJarify.jvm.JvmOps.IF_ICMPLE
import hu.oandras.kJarify.jvm.JvmOps.IF_ICMPLT
import hu.oandras.kJarify.jvm.JvmOps.IF_ICMPNE
import hu.oandras.kJarify.jvm.JvmOps.INVOKEINTERFACE
import hu.oandras.kJarify.jvm.JvmOps.INVOKESPECIAL
import hu.oandras.kJarify.jvm.JvmOps.INVOKESTATIC
import hu.oandras.kJarify.jvm.JvmOps.INVOKEVIRTUAL
import hu.oandras.kJarify.jvm.JvmOps.ISUB
import hu.oandras.kJarify.jvm.JvmOps.IXOR
import hu.oandras.kJarify.jvm.JvmOps.LCMP
import hu.oandras.kJarify.jvm.JvmOps.LXOR
import hu.oandras.kJarify.jvm.JvmOps.MONITORENTER
import hu.oandras.kJarify.jvm.JvmOps.MONITOREXIT
import hu.oandras.kJarify.jvm.JvmOps.PUTFIELD
import hu.oandras.kJarify.jvm.JvmOps.PUTSTATIC
import hu.oandras.kJarify.jvm.Scalars.isWide
import hu.oandras.kJarify.jvm.Scalars.paramTypes
import hu.oandras.kJarify.signExtend
import hu.oandras.kJarify.typeinference.TypeInfo

internal fun interface DalvikInstructionVisitor {

    fun invoke(
        method: Method,
        dex: DexFile,
        instructionDictionary: IntObjectMap<DalvikInstruction>,
        typeData: TypeInfo,
        block: IRBlock,
        instr: DalvikInstruction
    )

    private object VisitNop : DalvikInstructionVisitor {

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            // No operation
        }
    }

    private object VisitMove : DalvikInstructionVisitor {

        private val scalars: IntArray = intArrayOf(Scalars.INT, Scalars.OBJ, Scalars.FLOAT)

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val intArg1 = instr.getIntArg(1)

            for (st in scalars) {
                if (st and typeData.prims[intArg1] != 0) {
                    block.load(intArg1, st)
                    block.store(reg = instr.getIntArg(0), stype = st)
                }
            }
        }
    }

    private object VisitMoveWide : DalvikInstructionVisitor {

        private val scalars: IntArray = intArrayOf(Scalars.LONG, Scalars.DOUBLE)

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val intArg1 = instr.getIntArg(1)

            for (st in scalars) {
                if (st and typeData.prims[intArg1] != 0) {
                    block.load(intArg1, st)
                    block.store(instr.getIntArg(0), st)
                }
            }
        }
    }

    private object VisitMoveResult : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val st = Scalars.fromDesc(instr.prevResult!!)
            block.store(instr.getIntArg(0), st)
        }
    }

    private object VisitReturn : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            if (method.id.returnType.equalsWithChar('V')) {
                block.returnOp()
            } else {
                val st = Scalars.fromDesc(method.id.returnType)
                block.load(instr.getIntArg(0), st, method.id.returnType)
                block.returnOp(st)
            }
        }
    }

    private object VisitConst32 : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val value = instr.getIntArg(1)

            block.const32(value, Scalars.INT)
            block.store(instr.getIntArg(0), Scalars.INT)

            block.const32(value, Scalars.FLOAT)
            block.store(instr.getIntArg(0), Scalars.FLOAT)

            if (value == 0) {
                block.constNull()
                block.store(instr.getIntArg(0), Scalars.OBJ)
            }
        }
    }

    private object VisitConst64 : DalvikInstructionVisitor {

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val intArg0 = instr.getIntArg(0)
            val longArg1 = instr.getLongArg(1)

            block.const64(longArg1, Scalars.LONG)
            block.store(intArg0, Scalars.LONG)

            block.const64(longArg1, Scalars.DOUBLE)
            block.store(intArg0, Scalars.DOUBLE)
        }
    }

    private object VisitConstString : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val value = dex.string(instr.getIntArg(1))
            block.ldc(block.pool.stringRef(value))
            block.store(instr.getIntArg(0), Scalars.OBJ)
        }
    }

    private object VisitConstClass : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val value = dex.classType(instr.getIntArg(1))
            block.ldc(block.pool.classRef(value))
            block.store(instr.getIntArg(0), Scalars.OBJ)
        }
    }

    private object VisitMonitorEnter : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            block.load(instr.getIntArg(0), Scalars.OBJ)
            block.addOther(byteArrayOf_u8(MONITORENTER))
        }
    }

    private object VisitMonitorExit : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            block.load(instr.getIntArg(0), Scalars.OBJ)
            block.addOther(byteArrayOf_u8(MONITOREXIT))
        }

    }

    private object VisitCheckCast : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            block.cast(dex, instr.getIntArg(0), instr.getIntArg(1))
        }
    }

    private object VisitInstanceOf : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            block.load(instr.getIntArg(1), Scalars.OBJ)
            block.add(Other(
                byteArrayOf_u8u16(JvmOps.INSTANCEOF, block.pool.classRef(dex.classType(instr.getIntArg(2))))
            ))
            block.store(instr.getIntArg(0), Scalars.INT)
        }
    }

    private object VisitArrayLen : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            block.loadAsArray(instr.getIntArg(1))
            block.addOther(byteArrayOf_u8(ARRAYLENGTH))
            block.store(instr.getIntArg(0), Scalars.INT)
        }
    }

    private object VisitNewInstance : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            block.addOther(
                byteArrayOf_u8u16(
                    JvmOps.NEW,
                    block.pool.classRef(dex.classType(instr.getIntArg(1)))
                )
            )
            block.store(instr.getIntArg(0), Scalars.OBJ)
        }
    }

    private object VisitNewArray : DalvikInstructionVisitor {

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            block.load(instr.getIntArg(1), Scalars.INT)
            block.newArray(dex.type(instr.getIntArg(2)))
            block.store(instr.getIntArg(0), Scalars.OBJ)
        }
    }

    private object VisitFilledNewArray : DalvikInstructionVisitor {

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val intArg0 = instr.getIntArg(0)

            val regs: IntArray = instr.getIntArrayArg(1)
            block.const32(regs.size, Scalars.INT)

            val type = dex.type(intArg0)

            block.newArray(type)

            val stElet: EletPair = eletPair(ArrayTypes.fromDesc(type))
            val staticType = stElet.staticType
            val op = arrStoreOps.getOrDefault(ByteArrayKey(stElet.elet), AASTORE)

            val mustPop = instructionDictionary[instr.pos2]!!.type != MoveResult
            block.fillArraySub(op, regs.size, mustPop) { dataIndex ->
                block.load(reg = regs[dataIndex], staticType = staticType)
            }
        }
    }

    @Suppress("DEPRECATION")
    private object VisitFillArrayData : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val fillArrData = instructionDictionary[instr.getIntArg(1)]!!.fillArrData!!
            val arrayType = typeData.arrays[instr.getIntArg(0)]

            block.loadAsArray(instr.getIntArg(0))
            if (arrayType.contentEquals(ArrayTypes.NULL)) {
                block.addOther(byteArrayOf_u8(ATHROW))
            } else {
                if (fillArrData.size == 0) {
                    block.addOther(byteArrayOf_u8(ARRAYLENGTH))
                    block.add(Pop())
                } else {
                    val eletPair = eletPair(arrayType)
                    val st: Int = eletPair.staticType
                    val elet: ByteArray = eletPair.elet

                    val op = arrStoreOps[ByteArrayKey(elet)]

                    when (fillArrData) {
                        is ArrayData.ArrayData32 -> {
                            val origArrayData = fillArrData.arrayData

                            val arrayData = when (elet[0].toInt().toByte()) {
                                'B'.toInt().toByte(),
                                'Z'.toInt().toByte() -> {
                                    IntArray(origArrayData.size) {
                                        signExtend(origArrayData[it], 8)// and -0x1
                                    }
                                }

                                'S'.toInt().toByte() -> {
                                    IntArray(origArrayData.size) {
                                        signExtend(origArrayData[it], 16)// and -0x1
                                    }
                                }

                                else -> {
                                    origArrayData
                                }
                            }

                            block.fillArrayData(
                                op = op,
                                staticType = st,
                                values = arrayData
                            )
                        }
                        is ArrayData.ArrayData64 -> {
                            block.fillArrayData(
                                op = op,
                                staticType = st,
                                values = fillArrData.arrayData
                            )
                        }
                    }
                }
            }
        }
    }

    private object VisitThrow : DalvikInstructionVisitor {

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            block.load(instr.getIntArg(0), Scalars.OBJ, JAVA_LANG_THROWABLE)
            block.addOther(byteArrayOf_u8(ATHROW))
        }
    }

    private object VisitGoto : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            block.goto(instr.getIntArg(0))
        }
    }

    private object VisitSwitch : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            block.load(instr.getIntArg(0), Scalars.INT)
            val switchData = instructionDictionary[instr.getIntArg(1)]!!.switchData!!
            val defaultPos = instr.pos2
            val jumps = MutableIntIntMap(switchData.size)
            switchData.forEach { key, value ->
                jumps.put(key, /*(*/value + instr.position/*) % (1 shl 32)*/)
            }
            block.switchCase(defaultPos, jumps)
            jumps
        }
    }

    private object VisitCmp : DalvikInstructionVisitor {

        private val compareOpcodes: IntArray = intArrayOf(FCMPL, FCMPG, DCMPL, DCMPG, LCMP)

        private val compareStaticTypes: IntArray = intArrayOf(
            Scalars.FLOAT,
            Scalars.FLOAT,
            Scalars.DOUBLE,
            Scalars.DOUBLE,
            Scalars.LONG
        )

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val op = compareOpcodes[instr.opcode - 0x2d]
            val st = compareStaticTypes[instr.opcode - 0x2d]
            block.load(instr.getIntArg(1), st)
            block.load(instr.getIntArg(2), st)
            block.addOther(byteArrayOf_u8(op))
            block.store(instr.getIntArg(0), Scalars.INT)
        }
    }

    private object VisitIf : DalvikInstructionVisitor {

        private val primitiveIfOpcodes: IntArray = intArrayOf(IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE)
        private val classIfOpcodes: IntArray = intArrayOf(IF_ACMPEQ, IF_ACMPNE)

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val intArg0 = instr.getIntArg(0)
            val intArg1 = instr.getIntArg(1)
            val intArg2 = instr.getIntArg(2)

            val type = typeData.prims[intArg0] and typeData.prims[intArg1]
            if (type and Scalars.INT != 0) {
                block.load(intArg0, Scalars.INT)
                block.load(intArg1, Scalars.INT)
                val op = primitiveIfOpcodes[instr.opcode - 0x32]
                block.ifCondition(op, intArg2)
            } else {
                block.load(intArg0, Scalars.OBJ)
                block.load(intArg1, Scalars.OBJ)
                val op = classIfOpcodes[instr.opcode - 0x32]
                block.ifCondition(op, intArg2)
            }
        }
    }

    private object VisitIfZ : DalvikInstructionVisitor {

        private val primitiveIfZOpcodes: IntArray = intArrayOf(IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE)
        private val classIfZOpcodes: IntArray = intArrayOf(IFNULL, IFNONNULL)

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val arg0 = instr.getIntArg(0)
            val arg1 = instr.getIntArg(1)

            if (typeData.prims[arg0] and Scalars.INT != 0) {
                block.load(arg0, Scalars.INT)
                val op = primitiveIfZOpcodes[instr.opcode - 0x38]
                block.ifCondition(op, arg1)
            } else {
                block.load(arg0, Scalars.OBJ)
                val op = classIfZOpcodes[instr.opcode - 0x38]
                block.ifCondition(op, arg1)
            }
        }
    }

    private object VisitArrayGet : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val at = typeData.arrays[instr.getIntArg(1)]
            if (at.contentEquals(ArrayTypes.NULL)) {
                block.constNull()
                block.addOther(byteArrayOf_u8(ATHROW))
            } else {
                block.loadAsArray(instr.getIntArg(1))
                block.load(instr.getIntArg(2), Scalars.INT)

                val eletPair = eletPair(at)
                val st: Int = eletPair.staticType
                val elet: ByteArray = eletPair.elet
                block.addOther(
                    byteArrayOf_u8(
                        arrLoadOps.getOrDefault(ByteArrayKey(elet), AALOAD)
                    )
                )
                block.store(instr.getIntArg(0), st)
            }
        }
    }

    private object VisitArrayPut : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val at = typeData.arrays[instr.getIntArg(1)]
            if (at.contentEquals(ArrayTypes.NULL)) {
                block.constNull()
                block.addOther(byteArrayOf_u8(ATHROW))
            } else {
                block.loadAsArray(instr.getIntArg(1))
                block.load(instr.getIntArg(2), Scalars.INT)
                val eletPair = eletPair(at)
                val st: Int = eletPair.staticType
                val elet: ByteArray = eletPair.elet
                block.load(instr.getIntArg(0), st)
                block.addOther(byteArrayOf_u8(arrStoreOps.getOrDefault(ByteArrayKey(elet), AASTORE)))
            }
        }
    }

    private object VisitInstanceGet : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val fieldId = dex.fieldId(instr.getIntArg(2))
            val st = Scalars.fromDesc(fieldId.descriptor)
            block.load(instr.getIntArg(1), Scalars.OBJ, fieldId.className)
            block.addOther(
                byteArrayOf_u8u16(GETFIELD, block.pool.fieldRef(fieldId))
            )
            block.store(instr.getIntArg(0), st)
        }
    }

    private object VisitInstancePut : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val fieldId = dex.fieldId(instr.getIntArg(2))
            val st = Scalars.fromDesc(fieldId.descriptor)
            block.load(instr.getIntArg(1), Scalars.OBJ, fieldId.className)
            block.load(instr.getIntArg(0), st, fieldId.descriptor)
            block.addOther(
                byteArrayOf_u8u16(PUTFIELD, block.pool.fieldRef(fieldId))
            )
        }
    }

    private object VisitStaticGet : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val fieldId = dex.fieldId(instr.getIntArg(1))
            val st = Scalars.fromDesc(fieldId.descriptor)
            block.addOther(
                byteArrayOf_u8u16(GETSTATIC, block.pool.fieldRef(fieldId))
            )
            block.store(instr.getIntArg(0), st)
        }
    }

    private object VisitStaticPut : DalvikInstructionVisitor {
        
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val fieldId = dex.fieldId(instr.getIntArg(1))
            val st = Scalars.fromDesc(fieldId.descriptor)
            block.load(instr.getIntArg(0), st, fieldId.descriptor)
            block.addOther(
                byteArrayOf_u8u16(PUTSTATIC, block.pool.fieldRef(fieldId))
            )
        }
    }

    private object VisitInvoke : DalvikInstructionVisitor {
        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val isStatic = instr.type == InvokeStatic

            val calledId = dex.methodId(instr.getIntArg(0))
            val sts = paramTypes(calledId, isStatic)
            val descriptors: Array<ByteArray?> = calledId.getSpacedParameterTypes(isStatic)
            assert(sts.size == instr.getIntArrayArg(1).size && sts.size == descriptors.size)

            for (i in 0 until sts.size) {
                val st = sts[i]
                val desc = descriptors[i]
                val reg: Int = instr.getIntArrayArg(1)[i]
                if (st != Scalars.INVALID) { // skip long/double tops
                    block.load(
                        reg = reg,
                        staticType = st,
                        desc = desc
                    )
                }
            }

            val op: Int = when (instr.type) {
                InvokeVirtual -> INVOKEVIRTUAL
                InvokeSuper, InvokeDirect -> INVOKESPECIAL
                InvokeStatic -> INVOKESTATIC
                InvokeInterface -> INVOKEINTERFACE
                else -> throw IllegalStateException("Unexpected value: " + instr.type)
            }

            val pool = block.pool

            if (instr.type == InvokeInterface) {
                block.add(
                    InvokeInterface(op, pool.interfaceMethodRef(calledId), descriptors)
                )
            } else {
                block.add(
                    Invoke(op, pool.methodRef(calledId))
                )
            }

            // check if we need to pop result instead of leaving on stack
            if (instructionDictionary[instr.pos2]!!.type != MoveResult) {
                val returnType = calledId.returnType
                if (!returnType.equalsWithChar('V')) {
                    val st = Scalars.fromDesc(returnType)
                    block.add(if (isWide(st)) Pop2() else Pop())
                }
            }
        }
    }

    private object VisitUnaryOp : DalvikInstructionVisitor {

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val opInfo: IntArray = MathOps.UNARY[instr.opcode]!!
            val op = opInfo[0]
            val sourceType = opInfo[1]
            val destinationType = opInfo[2]
            block.load(instr.getIntArg(1), sourceType)
            // *not requires special handling since there's no direct Java equivalent. Instead, we have to do x ^ -1
            if (op == IXOR) {
                block.addOther(byteArrayOf_u8(ICONST_M1))
            } else if (op == LXOR) {
                block.addOther(byteArrayOf_u8(ICONST_M1))
                block.addOther(byteArrayOf_u8(I2L))
            }

            block.addOther(byteArrayOf_u8(op))
            block.store(instr.getIntArg(0), destinationType)
        }
    }

    private object VisitBinaryOp : DalvikInstructionVisitor {

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val opInfo: IntArray = MathOps.BINARY[instr.opcode]!!
            val op = opInfo[0]
            val st = opInfo[1]
            val st2 = opInfo[2]

            val argsSize = instr.getArgsSize()
            block.load(instr.getIntArg(argsSize - 2), st)
            block.load(instr.getIntArg(argsSize - 1), st2)

            block.addOther(byteArrayOf_u8(op))
            block.store(instr.getIntArg(0), st)
        }
    }

    private object VisitBinaryOpConst : DalvikInstructionVisitor {

        override fun invoke(
            method: Method,
            dex: DexFile,
            instructionDictionary: IntObjectMap<DalvikInstruction>,
            typeData: TypeInfo,
            block: IRBlock,
            instr: DalvikInstruction
        ) {
            val op: Int = MathOps.BINARY_LIT[instr.opcode]
            if (op == ISUB) { // rsub
                block.const32(instr.getIntArg(2), Scalars.INT)
                block.load(instr.getIntArg(1), Scalars.INT)
            } else {
                block.load(instr.getIntArg(1), Scalars.INT)
                block.const32(instr.getIntArg(2), Scalars.INT)
            }
            block.addOther(byteArrayOf_u8(op))
            block.store(instr.getIntArg(0), Scalars.INT)
        }
    }

    companion object {

        @JvmField
        val VISIT_FUNCS: Map<DalvikOpcode, DalvikInstructionVisitor> = mapOf(
            Nop to VisitNop,
            Move to VisitMove,
            MoveWide to VisitMoveWide,
            MoveResult to VisitMoveResult,
            Return to VisitReturn,
            Const32 to VisitConst32,
            Const64 to VisitConst64,
            ConstString to VisitConstString,
            ConstClass to VisitConstClass,
            MonitorEnter to VisitMonitorEnter,
            MonitorExit to VisitMonitorExit,
            CheckCast to VisitCheckCast,
            InstanceOf to VisitInstanceOf,
            ArrayLen to VisitArrayLen,
            NewInstance to VisitNewInstance,
            NewArray to VisitNewArray,
            FilledNewArray to VisitFilledNewArray,
            FillArrayData to VisitFillArrayData,
            Throw to VisitThrow,
            Goto to VisitGoto,
            Switch to VisitSwitch,
            Cmp to VisitCmp,
            If to VisitIf,
            IfZ to VisitIfZ,

            ArrayGet to VisitArrayGet,
            ArrayPut to VisitArrayPut,
            InstanceGet to VisitInstanceGet,
            InstancePut to VisitInstancePut,
            StaticGet to VisitStaticGet,
            StaticPut to VisitStaticPut,

            InvokeVirtual to VisitInvoke,
            InvokeSuper to VisitInvoke,
            InvokeDirect to VisitInvoke,
            InvokeStatic to VisitInvoke,
            InvokeInterface to VisitInvoke,

            UnaryOp to VisitUnaryOp,
            BinaryOp to VisitBinaryOp,
            BinaryOpConst to VisitBinaryOpConst,
        )
    }
}
