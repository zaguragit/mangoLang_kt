package mango.compilation.llvm

import mango.interpreter.symbols.TypeSymbol

interface LLVMType {
    val code: String
    val isFloatingPoint: Boolean get() = false
    val isInteger: Boolean get() = false

    companion object {
        fun valueOf(type: TypeSymbol) = when (type) {
            //TypeSymbol.any ->
            TypeSymbol.int -> I32
            TypeSymbol.bool -> Bool
            TypeSymbol.string -> Pointer(I8)
            TypeSymbol.unit -> Void
            else -> throw Exception("internal error: type unknown to LLVM")
        }
    }

    object Bool : LLVMType {
        override val isInteger = true
        override val code = "i1"
    }

    object I8 : LLVMType {
        override val isInteger = true
        override val code = "i8"
    }

    object I16 : LLVMType {
        override val isInteger = true
        override val code = "i16"
    }

    object I32 : LLVMType {
        override val isInteger = true
        override val code = "i32"
    }

    object I64 : LLVMType {
        override val isInteger = true
        override val code = "i64"
    }

    object Float : LLVMType {
        override val isFloatingPoint = true
        override val code = "float"
    }

    object Double : LLVMType {
        override val isFloatingPoint = true
        override val code = "double"
    }

    data class Pointer(
        val element: LLVMType
    ) : LLVMType {
        override val code = "${element.code}*"
    }

    object Void : LLVMType {
        override val code = "void"
    }

    class CustomType(
        override val code: String
    ) : LLVMType
}
