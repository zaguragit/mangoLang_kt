package mango.compilation.llvm

import mango.compilation.llvm.LLVMValue.LocalValRef
import mango.interpreter.symbols.FunctionSymbol
import java.util.*

interface LLVMInstruction {
    val code: String
    val type: LLVMType?
}

class ReturnInt(val value: Int) : LLVMInstruction {
    override val type = null
    override val code get() = "ret i32 $value"
}

class Return(val value: LLVMValue) : LLVMInstruction {
    override val type get() = null
    override val code get() = "ret ${value.type.code} ${value.code}"
}

class ReturnVoid : LLVMInstruction {
    override val code = "ret void"
    override val type = LLVMType.Void
}

class Load(val value: LLVMValue) : LLVMInstruction {
    override val code get() = "load ${type.code}, ${value.type.code} ${value.code}"
    override val type get() = (value.type as LLVMType.Pointer).element
}

class IfInstruction(val condition: LLVMValue, val yesLabel: String, val noLabel: String) : LLVMInstruction {
    override val code get() = "br ${condition.type.code} ${condition.code}, label %${yesLabel}, label %${noLabel}"
    override val type = null
}

class JumpInstruction(val label: String) : LLVMInstruction {
    override val code get() = "br label %${label}"
    override val type = null
}

enum class ComparisonType(val code: String) {
    LessThan("slt"),
    MoreThan("sgt"),
    IsEqual("eq"),
    IsEqualOrMore("sge"),
    IsEqualOrLess("sle"),
    IsNotEqual("ne")
}

data class Comparison(val comparisonType: ComparisonType, val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "icmp ${comparisonType.code} ${left.type.code} ${left.code}, ${right.code}"
    override val type = LLVMType.Bool
}

class TempValue(val name: String, val value: LLVMInstruction) : LLVMInstruction {
    override val code get(): String = "%$name = ${value.code}"
    fun reference() = LocalValRef(name, value.type!!)
    override val type get() = value.type!!
}

class Store(val value: LLVMValue, val destination: LLVMValue) : LLVMInstruction {
    override val code get() = "store ${value.type.code} ${value.code}, ${destination.type.code} ${destination.code}"
    override val type = null
}

class GetElementPtr(val privType: LLVMType, val pointer: LLVMValue, val index: LLVMValue) : LLVMInstruction {
    override val code get() = "getelementptr inbounds ${type.code}, ${pointer.type.code} ${pointer.code}, i64 ${index.code}"
    override val type get() = LLVMType.Pointer(privType)
}

class Call(
    val returnType: LLVMType,
    val symbol: FunctionSymbol,
    vararg val params: LLVMValue
) : LLVMInstruction {
    override val code get() = "call ${returnType.code} @${symbol.meta.cName ?: symbol.path}(${params.joinToString(separator = ", ") { "${it.type.code} ${it.code}" }})"
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

class SignedIntDivision(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "sdiv ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class UnsignedIntDivision(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "udiv ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class FloatDivision(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "fdiv ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class IntMultiplication(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "mul ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class FloatMultiplication(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "fmul ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class IntAddition(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "add ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class FloatAddition(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "fadd ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class IntSubtraction(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "sub ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class FloatSubtraction(val left: LLVMValue, val right: LLVMValue) : LLVMInstruction {
    override val code get() = "fsub ${type.code} ${left.code}, ${right.code}"
    override val type = left.type
}

class ConversionFloatToSignedInt(val value: LLVMValue, val targetType: LLVMType) : LLVMInstruction {
    override val code get() = "fptosi ${value.type.code} ${value.code} to ${targetType.code}"
    override val type = targetType
}

class ConversionFloatToUnsignedInt(val value: LLVMValue, val targetType: LLVMType) : LLVMInstruction {
    override val code get() = "fptoui ${value.type.code} ${value.code} to ${targetType.code}"
    override val type = targetType
}

class ConversionSignedIntToFloat(val value: LLVMValue, val targetType: LLVMType) : LLVMInstruction {
    override val code get() = "sitofp ${value.type.code} ${value.code} to ${targetType.code}"
    override val type = targetType
}

class ConversionUnsignedIntToFloat(val value: LLVMValue, val targetType: LLVMType) : LLVMInstruction {
    override val code get() = "uitofp ${value.type.code} ${value.code} to ${targetType.code}"
    override val type = targetType
}
