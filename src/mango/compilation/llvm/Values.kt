package mango.compilation.llvm

interface LLVMValue {
    val code: String
    val type: LLVMType
}

class StringReference(
    val stringConst: StringConst
) : LLVMValue {
    override val type get() = LLVMType.Pointer(LLVMType.I8)
    override val code get() = "getelementptr inbounds ([${stringConst.lengthInBytes()} x i8], [${stringConst.lengthInBytes()} x i8]* @${stringConst.id}, i32 0, i32 0)"
}

data class LocalValueRef(
    val name: String,
    override val type: LLVMType
) : LLVMValue {
    override val code get() = "%$name"
}

data class GlobalValueRef(
    val name: String,
    val privType: LLVMType
) : LLVMValue {
    override val type get() = LLVMType.Pointer(privType)
    override val code get() = "@$name"
}

class BoolConst(
    val value: Boolean
) : LLVMValue {
    override val type = LLVMType.Bool
    override val code get() = "${if (value) 1 else 0}"
}

class IntConst(
    val value: Int,
    override val type: LLVMType
) : LLVMValue {
    override val code get() = "$value"
}

class FloatConst(
    val value: Float,
    override val type: LLVMType
) : LLVMValue {
    override val code get() = "$value"
}

data class Null(
    override val type: LLVMType
) : LLVMValue {
    override val code get() = "null"
}