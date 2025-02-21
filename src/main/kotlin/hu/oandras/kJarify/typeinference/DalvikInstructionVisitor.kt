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

package hu.oandras.kJarify.typeinference

import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import hu.oandras.kJarify.dex.DalvikInstruction
import hu.oandras.kJarify.dex.DalvikOpcode
import hu.oandras.kJarify.dex.DexFile
import hu.oandras.kJarify.dex.FieldId
import hu.oandras.kJarify.jvm.ArrayTypes
import hu.oandras.kJarify.jvm.MathOps
import hu.oandras.kJarify.jvm.Scalars

internal fun interface DalvikInstructionVisitor {

    fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo

    // Lots of instructions just return an object or int for type inference purposes
    // so we have a single function for these cases
    private object VisitRetObj : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            return cur.assign(instr.getIntArg(0), Scalars.OBJ)
        }
    }

    private object VisitRetInt : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            return cur.assign(instr.getIntArg(0), Scalars.INT)
        }
    }

    // Instruction specific callbacks
    private object VisitMove : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            return cur.move(instr.getIntArg(1), instr.getIntArg(0), false)
        }
    }

    private object VisitMoveWide : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            return cur.move(instr.getIntArg(1), instr.getIntArg(0), true)
        }
    }

    private object VisitMoveResult : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            return cur.assignFromDesc(instr.getIntArg(0), instr.prevResult!!)
        }
    }

    private object VisitConst32 : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            val value: Long = instr.getIntArg(1) % (1L shl 32)
            return if (value == 0L) {
                cur.assign(instr.getIntArg(0), Scalars.ZERO, ArrayTypes.NULL)
            } else {
                cur.assign(instr.getIntArg(0), Scalars.C32)
            }
        }
    }

    private object VisitConst64 : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            return cur.assign2(instr.getIntArg(0), Scalars.C64)
        }
    }

    private object VisitCheckCast : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            val arg0 = instr.getIntArg(0)
            val arg1 = instr.getIntArg(1)

            var at: ByteArray = ArrayTypes.fromDesc(dex.type(arg1))
            at = ArrayTypes.narrow(cur.arrays[arg0], at)
            return cur.assign(arg0, Scalars.OBJ, at)
        }
    }

    private object VisitNewArray : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            val at: ByteArray = ArrayTypes.fromDesc(dex.type(instr.getIntArg(2)))
            return cur.assign(instr.getIntArg(0), Scalars.OBJ, at)
        }
    }

    private object VisitArrayGet : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            val arrAt: ByteArray = cur.arrays[instr.getIntArg(1)]
            if (arrAt.contentEquals(ArrayTypes.NULL)) {
                // This is unreachable, so use (ALL, NULL), which can be merged with anything
                return cur.assign(instr.getIntArg(0), Scalars.ALL, ArrayTypes.NULL)
            } else {
                val pair = ArrayTypes.eletPair(arrAt)
                return cur.assign(instr.getIntArg(0), pair.staticType, pair.elet)
            }
        }
    }

    private object VisitInstanceGet : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            val fieldId: FieldId = dex.fieldId(instr.getIntArg(2))
            return cur.assignFromDesc(instr.getIntArg(0), fieldId.descriptor)
        }
    }

    private object VisitStaticGet : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            val fieldId: FieldId = dex.fieldId(instr.getIntArg(1))
            return cur.assignFromDesc(instr.getIntArg(0), fieldId.descriptor)
        }
    }

    private object VisitUnaryOp : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            val unaryOp: IntArray = MathOps.UNARY[instr.opcode]!!
            val st: Int = unaryOp[2]
            return if (Scalars.isWide(st)) {
                cur.assign2(instr.getIntArg(0), st)
            } else {
                cur.assign(instr.getIntArg(0), st)
            }
        }
    }

    private object VisitBinaryOp : DalvikInstructionVisitor {
        override fun invoke(dex: DexFile, instr: DalvikInstruction, cur: TypeInfo): TypeInfo {
            val binaryOp: IntArray = MathOps.BINARY[instr.opcode]!!
            val st: Int = binaryOp[1]
            return if (Scalars.isWide(st)) {
                cur.assign2(instr.getIntArg(0), st)
            } else {
                cur.assign(instr.getIntArg(0), st)
            }
        }
    }

    companion object {

        @JvmField
        val FUNCS: ArrayMap<DalvikOpcode, DalvikInstructionVisitor> = arrayMapOf(
            DalvikOpcode.ConstString to VisitRetObj,
            DalvikOpcode.ConstClass to VisitRetObj,
            DalvikOpcode.NewInstance to VisitRetObj,
            DalvikOpcode.InstanceOf to VisitRetInt,
            DalvikOpcode.ArrayLen to VisitRetInt,
            DalvikOpcode.Cmp to VisitRetInt,
            DalvikOpcode.BinaryOpConst to VisitRetInt,

            DalvikOpcode.Move to VisitMove,
            DalvikOpcode.MoveWide to VisitMoveWide,
            DalvikOpcode.MoveResult to VisitMoveResult,
            DalvikOpcode.Const32 to VisitConst32,
            DalvikOpcode.Const64 to VisitConst64,
            DalvikOpcode.CheckCast to VisitCheckCast,
            DalvikOpcode.NewArray to VisitNewArray,
            DalvikOpcode.ArrayGet to VisitArrayGet,
            DalvikOpcode.InstanceGet to VisitInstanceGet,
            DalvikOpcode.StaticGet to VisitStaticGet,
            DalvikOpcode.UnaryOp to VisitUnaryOp,
            DalvikOpcode.BinaryOp to VisitBinaryOp,
        )
    }
}