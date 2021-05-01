package mango.cli.emission.llvm

import mango.cli.emission.llvm.LLVMEmitter.LLVMEmitterError
import mango.compiler.symbols.Symbol
import mango.compiler.symbols.TypeSymbol

interface LLVMType {
    val code: String
    val isFloatingPoint: Boolean get() = false
    val isInteger: Boolean get() = false
    val bits: Int

    companion object {

        operator fun get(type: TypeSymbol.StructTypeSymbol) = Ptr(Struct(type.path, type.size))
        operator fun get(type: TypeSymbol.Fn) = Ptr(Fn(get(type.returnType), type.args.map { get(it) }))
        operator fun get(type: TypeSymbol): LLVMType {
            return when (type.kind) {
                Symbol.Kind.StructType -> get(type as TypeSymbol.StructTypeSymbol)
                Symbol.Kind.FunctionType -> get(type as TypeSymbol.Fn)
                else -> when (type.path) {
                    TypeSymbol.Any.path -> Ptr(I8)
                    TypeSymbol.I8.path -> I8
                    //TypeSymbol.U8 -> U8
                    TypeSymbol.I16.path -> I16
                    //TypeSymbol.U16 -> U16
                    TypeSymbol.I32.path -> I32
                    //TypeSymbol.U32 -> U32
                    TypeSymbol.I64.path -> I64
                    //TypeSymbol.U64 -> U64
                    TypeSymbol.Float.path -> Float
                    TypeSymbol.Double.path -> Double
                    TypeSymbol.Bool.path -> Bool
                    TypeSymbol.Ptr.path -> Ptr(get(type.params[0]))
                    TypeSymbol.Void.path -> Void
                    TypeSymbol.Char.path -> I16
                    else -> throw LLVMEmitterError("internal error: type unknown to LLVM (${type.path})")
                }
            }
        }
    }

    abstract class I(final override val bits: Int) : LLVMType {
        override val isInteger = true
        override val code = "i$bits"
    }

    open class U(bits: Int) : I(bits) {
        override val code = "u$bits"
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

    data class Ptr<T : LLVMType>(
        val element: T
    ) : LLVMType {
        override val code = "${element.code}*"
        override val bits = element.bits
    }

    class Struct(
        val name: String,
        override val bits: Int
    ) : LLVMType {
        override val code = "%\".type.$name\""
    }

    class Fn(
        val returnType: LLVMType,
        val args: Collection<LLVMType>
    ) : LLVMType {
        override val code = "${returnType.code}(${args.joinToString(", ") { it.code }})"
        override val bits = 0
    }
}

fun max(t0: LLVMType, t1: LLVMType) = if (t0.bits > t1.bits) t0 else t1