package mango.compilation.llvm

import mango.interpreter.symbols.Symbol
import mango.interpreter.symbols.TypeSymbol
import mango.util.EmitterError

interface LLVMType {
    val code: String
    val isFloatingPoint: Boolean get() = false
    val isInteger: Boolean get() = false
    val bits: Int

    companion object {

        operator fun get(type: TypeSymbol.StructTypeSymbol) = Ptr(Struct(type.name))
        operator fun get(type: TypeSymbol): LLVMType {
            return when (type.kind) {
                Symbol.Kind.Struct -> Ptr(Struct(type.name))
                Symbol.Kind.FunctionType -> {
                    type as TypeSymbol.Fn
                    Fn(get(type.returnType), type.args.map { get(it) })
                }
                else -> when (type.name) {
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
                    TypeSymbol.Ptr.name -> Ptr(get(type.params[0]))
                    TypeSymbol.Unit.name -> Void
                    else -> throw EmitterError("internal error: type unknown to LLVM (${type.name})")
                }
            }
        }
    }

    open class I(final override val bits: Int) : LLVMType {
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
        override val bits = 32
        override val code = "float"
    }

    object Double : LLVMType {
        override val isFloatingPoint = true
        override val bits = 64
        override val code = "double"
    }

    object Void : LLVMType {
        override val code = "void"
        override val bits = 0
    }

    data class Ptr(
        val element: LLVMType
    ) : LLVMType {
        override val code = "${element.code}*"
        override val bits = element.bits
    }

    class Struct(
        val name: String
    ) : LLVMType {
        override val code = "%.struct.$name"
        override val bits = 0
    }

    class Fn(
        val returnType: LLVMType,
        args: Collection<LLVMType>
    ) : LLVMType {
        override val code = "${returnType.code}(${args.joinToString(", ") { it.code }})"
        override val bits = 0
    }
}

fun max(t0: LLVMType, t1: LLVMType) = if (t0.bits > t1.bits) t0 else t1