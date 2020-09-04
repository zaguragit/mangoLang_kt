package mango.compilation.llvm

interface LLVMValue {

    val code: String
    val type: LLVMType

    class ArrayRef(
        val arrayConst: ArrayConst
    ) : LLVMValue {
        override val type get() = LLVMType.Ptr(arrayConst.type)
        override val code get() = "getelementptr inbounds ([${arrayConst.lengthInBytes()} x ${arrayConst.type.code}], [${arrayConst.lengthInBytes()} x ${arrayConst.type.code}]* @${arrayConst.id}, i64 0, i32 0)"
    }

    class LocalRef(
        var name: String,
        val privType: LLVMType
    ) : LLVMValue {
        override val type get() = privType
        override val code get() = "%$name"
    }

    class GlobalRef(
        val name: String,
        val privType: LLVMType
    ) : LLVMValue {
        override val type get() = LLVMType.Ptr(privType)
        override val code get() = "@\"$name\""
    }

    class Bool(
        val value: Boolean
    ) : LLVMValue {
        override val type = LLVMType.Bool
        override val code get() = "${if (value) 1 else 0}"
    }

    class Int(
        val value: kotlin.Int,
        override val type: LLVMType
    ) : LLVMValue {
        override val code get() = "$value"
    }

    class Float(
        val value: kotlin.Float,
        override val type: LLVMType
    ) : LLVMValue {
        override val code get() = "$value"
    }

    class Null(
        override val type: LLVMType = LLVMType.Void
    ) : LLVMValue {
        override val code get() = "null"
    }

    class Struct(
        override val type: LLVMType.Struct,
        val values: Array<LLVMValue>
    ) : LLVMValue {
        override val code get() = "{ " + values.joinToString(", ") { it.type.code + ' ' + it.code } + " }"
    }
}

