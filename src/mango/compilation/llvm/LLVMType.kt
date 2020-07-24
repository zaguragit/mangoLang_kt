package mango.compilation.llvm

import mango.interpreter.symbols.Symbol
import mango.interpreter.symbols.TypeSymbol

interface LLVMType {
    val code: String
    val isFloatingPoint: Boolean get() = false
    val isInteger: Boolean get() = false

    companion object {

        fun valueOf(type: TypeSymbol.StructTypeSymbol) = Struct(type.name)
        fun valueOf(type: TypeSymbol): LLVMType {
            return if (type.kind == Symbol.Kind.Struct) {
                Struct(type.name)
            } else when (type.name) {
                TypeSymbol.Any.name -> Ptr(Void)
                TypeSymbol.I8.name -> I8
                //TypeSymbol.U8 -> U8
                TypeSymbol.I16.name -> I16
                //TypeSymbol.U16 -> U16
                TypeSymbol.I32.name -> I32
                //TypeSymbol.U32 -> U32
                TypeSymbol.I64.name -> I64
                //TypeSymbol.U64 -> U64
                TypeSymbol.Float.name -> Float
                TypeSymbol.Double.name -> Double
                TypeSymbol.Bool.name -> Bool
                TypeSymbol.Ptr.name -> Ptr(valueOf(type.params[0]))
                TypeSymbol.Unit.name -> Void
                else -> throw Exception("internal error: type unknown to LLVM (${type.name})")
            }
        }
    }

    open class I(bits: Int) : LLVMType {
        override val isInteger = true
        override val code = "i$bits"
    }

    object Bool : I(1)
    object I8   : I(8)
    object I16  : I(16)
    object I32  : I(32)
    object I64  : I(64)

    object Float : LLVMType {
        override val isFloatingPoint = true
        override val code = "float"
    }

    object Double : LLVMType {
        override val isFloatingPoint = true
        override val code = "double"
    }

    object Void : LLVMType {
        override val code = "void"
    }

    data class Ptr(
        val element: LLVMType
    ) : LLVMType {
        override val code = "${element.code}*"
    }

    class Struct(
        val name: String
    ) : LLVMType {
        override val code = "%.struct.$name"
    }
}
