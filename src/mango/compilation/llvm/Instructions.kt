package mango.compilation.llvm

import mango.compilation.llvm.LLVMValue.LocalRef
import mango.interpreter.symbols.CallableSymbol
import mango.interpreter.symbols.VisibleSymbol
import java.util.*

interface LLVMInstruction {
    val code: String
    val type: LLVMType
}

val LLVMInstruction?.isJump get() =
    this !is Ret &&
    this !is RetVoid &&
    this !is Jmp &&
    this !is If

class Ret(val value: LLVMValue) : LLVMInstruction {
    override val type get() = LLVMType.Void
    override val code get() = "ret ${value.type.code} ${value.code}"
}

class RetVoid : LLVMInstruction {
    override val code = "ret void"
    override val type = LLVMType.Void
}

class Load(val value: LLVMValue, override val type: LLVMType) : LLVMInstruction {
    override val code get() = "load ${type.code}, ${LLVMType.Ptr(type).code} ${value.code}"
}

class If(
    val condition: LLVMValue,
    val yesLabel: String,
    val noLabel: String
) : LLVMInstruction {
    override val code get() = "br ${condition.type.code} ${condition.code}, label %${yesLabel}, label %${noLabel}"
    override val type = LLVMType.Void
}

class Jmp(
    val label: String
) : LLVMInstruction {
    override val code get() = "br label %${label}"
    override val type = LLVMType.Void
}

data class Icmp(
    val comparisonType: Type,
    val left: LLVMValue,
    val right: LLVMValue
) : LLVMInstruction {

    override val code get() = "icmp ${comparisonType.code} ${left.type.code} ${left.code}, ${right.code}"
    override val type = LLVMType.Bool

    enum class Type(val code: String) {
        LessThan("slt"),
        MoreThan("sgt"),
        IsEqual("eq"),
        IsEqualOrMore("sge"),
        IsEqualOrLess("sle"),
        IsNotEqual("ne")
    }
}

class TmpVal(
    val name: String,
    val value: LLVMInstruction
) : LLVMInstruction {
    override val code get(): String = "%$name = ${value.code}"
    val ref get() = LocalRef(name, value.type)
    override val type get() = value.type
}

class Store(
    val value: LLVMValue,
    val destination: LLVMValue
) : LLVMInstruction {
    override val code get() = "store ${value.type.code} ${value.code}, ${destination.type.code} ${destination.code}"
    override val type = LLVMType.Void
}

class GetPtr(
    override val type: LLVMType,
    val pointer: LLVMValue,
    val index: LLVMValue
) : LLVMInstruction {
    override val code get() = "getelementptr inbounds ${type.code}, ${pointer.type.code} ${pointer.code}, i64 0, ${index.type.code} ${index.code}"
}

class Call(
    val returnType: LLVMType,
    val symbol: CallableSymbol,
    vararg val params: LLVMValue
) : LLVMInstruction {
    override val code get() = "call ${returnType.code} @${symbol.meta.cname ?: if (symbol is VisibleSymbol) symbol.path else symbol.name}(${params.joinToString(separator = ", ") { "${it.type.code} ${it.code}" }})"
    override val type = returnType
}

class CallWithBitCast(val declaration: FunctionDeclaration, private vararg val params: LLVMValue) : LLVMInstruction {
    override val code: String get() {
        val argTypesStrs = LinkedList<String>()
        params.forEach { argTypesStrs.add(it.type.code) }
        if (declaration.varargs) {
            argTypesStrs.add("...")
        }
        val adaptedSignature = "${declaration.returnType.code} (${argTypesStrs.joinToString(separator = ", ")})"
        val paramsStr = params.joinToString(separator = ", ") { "${it.type.code} ${it.code}" }
        return "call $adaptedSignature bitcast (${declaration.ptrSignature()} @${declaration.name} to $adaptedSignature*)($paramsStr)"
    }
    override val type get() = declaration.returnType
}

class IntDiv(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "sdiv ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class UIntDiv(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "udiv ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class FloatDiv(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "fdiv ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class IntMul(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "mul ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class FloatMul(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "fmul ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class IntAdd(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "add ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class FloatAdd(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "fadd ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class IntSub(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "sub ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class FloatSub(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "fsub ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class FloatToInt(val value: LLVMValue, val targetType: LLVMType) : LLVMInstruction {
    override val code get() = "fptosi ${value.type.code} ${value.code} to ${targetType.code}"
    override val type = targetType
}

class FloatToUInt(val value: LLVMValue, val targetType: LLVMType) : LLVMInstruction {
    override val code get() = "fptoui ${value.type.code} ${value.code} to ${targetType.code}"
    override val type = targetType
}

class IntToFloat(val value: LLVMValue, val targetType: LLVMType) : LLVMInstruction {
    override val code get() = "sitofp ${value.type.code} ${value.code} to ${targetType.code}"
    override val type = targetType
}

class UIntToFloat(val value: LLVMValue, val targetType: LLVMType) : LLVMInstruction {
    override val code get() = "uitofp ${value.type.code} ${value.code} to ${targetType.code}"
    override val type = targetType
}
