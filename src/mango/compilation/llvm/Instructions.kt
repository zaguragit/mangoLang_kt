package mango.compilation.llvm

import mango.compilation.llvm.LLVMValue.LocalRef
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

class Alloc(
    val privType: LLVMType
) : LLVMInstruction {
    override val type = LLVMType.Ptr(privType)
    override val code get() = "alloca ${privType.code}"
}

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
    name: String,
    val value: LLVMInstruction
) : LLVMInstruction {
    override val code get(): String = "%$name = ${value.code}"
    val ref = LocalRef(name, value.type)
    val name get() = ref.name
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
    val privType: LLVMType,
    val pointer: LLVMValue,
    val pointerI: LLVMValue,
    val fieldI: LLVMValue? = null
) : LLVMInstruction {
    override val type = LLVMType.Ptr(privType)
    override val code get() = "getelementptr inbounds ${privType.code}, ${pointer.type.code} ${pointer.code}, ${pointerI.type.code} ${pointerI.code}".let {
        if (fieldI == null) it else {
            it + ", ${fieldI.type.code} ${fieldI.code}"
        }
    }
}

class Call(
    val returnType: LLVMType,
    val function: LLVMValue,
    vararg val params: LLVMValue
) : LLVMInstruction {
    override val code get() = "call ${returnType.code} ${function.code}(${params.joinToString(", ") { it.type.code + ' ' + it.code } })"
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

class Operation(
    val kind: Kind,
    val left: LLVMValue,
    val right: LLVMValue
) : LLVMInstruction {

    override val code get() = "${kind.code} ${type.code} ${left.code}, ${right.code}"
    override val type = left.type

    enum class Kind (val code: String) {
        IntDiv("sdiv"),
        UIntDiv("udiv"),
        FloatDiv("fdiv"),
        IntMul("mul"),
        FloatMul("fmul"),
        IntAdd("add"),
        FloatAdd("fadd"),
        IntSub("sub"),
        FloatSub("fsub")
    }
}

class Conversion(
    val kind: Kind,
    val value: LLVMValue,
    val targetType: LLVMType
) : LLVMInstruction {

    override val code get() = "${kind.code} ${value.type.code} ${value.code} to ${targetType.code}"
    override val type = targetType

    enum class Kind (val code: String) {
        FloatToInt("fptosi"),
        FloatToUInt("fptoui"),
        IntToFloat("sitofp"),
        UIntToFloat("uitofp"),
        ZeroExt("zext"),
        SignExt("sext"),
        FloatExt("fpext")
    }
}