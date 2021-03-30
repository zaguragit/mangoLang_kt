package mango.compiler.symbols

import mango.compiler.binding.Scope
import mango.util.BinderError

open class TypeSymbol private constructor(
        val path: String,
        open val parentType: TypeSymbol?,
        open val size: Int = 0,
        val paramCount: Int = 0,
        val params: Array<TypeSymbol> = arrayOf()
) : Symbol() {

    override val name: String = path.substringAfterLast('.')
    override val type get() = null!!

    override val kind = Kind.Type

    class StructTypeSymbol(
        path: String,
        val declarationScope: Scope
    ) : TypeSymbol(path, null) {

        override val kind = Kind.StructType

        override val size get() = fields.sumBy {
            it.type.size
        } + (parentType?.size ?: 0)

        override var parentType: TypeSymbol? = null

        var fields: Array<Field> = arrayOf()

        fun getAllRealFields() = Array(getFieldCount()) { getField(it) }

        fun getField(name: String): Field? {
            return fields.find { it.name == name } ?: run {
                val p = parentType
                if (p is StructTypeSymbol) p.getField(name) else null
            }
        }

        fun getField(i: Int): Field {
            val index = i - getFieldCount() + fields.size
            val p = parentType
            return if (index < 0 && p is StructTypeSymbol) p.getField(i) else fields[index]
        }

        fun getFieldCount(): Int = fields.count { !it.isOverride }.let {
            val p = parentType
            if (p is StructTypeSymbol) it + p.getFieldCount() else it
        }

        fun getFieldI(name: String): Int {
            return fields.indexOfFirst { it.name == name && !it.isOverride }.let {
                if (it == -1) {
                    val p = parentType
                    if (p is StructTypeSymbol) p.getFieldI(name) else -1
                } else it + getFieldCount() - fields.size
            }
        }

        open class Field(
                val name: String,
                val type: TypeSymbol,
                val isReadOnly: Boolean,
                val isOverride: Boolean
        ) {
            override fun toString() = (if (isOverride) "[override]" else "") + (if (isReadOnly) "val " else "var ") + name + ' ' + type
        }
    }

    class Fn(
            val returnType: TypeSymbol,
            val args: List<TypeSymbol>
    ) : TypeSymbol("!Fn", Any, ptrSize) {

        override val kind = Kind.FunctionType
        override fun isOfType(other: TypeSymbol): Boolean = other is Fn || Any.isOfType(other)

        override fun toString() = "$returnType(${args.joinToString(", ")})"

        companion object {
            val entry = Fn(Void, listOf())
        }
    }

    companion object {

        val builtin = ArrayList<TypeSymbol>()
        val declared = ArrayList<StructTypeSymbol>()

        private inline fun createBuiltinType(
                name: String,
                parentType: TypeSymbol?,
                size: Int = 0,
                paramCount: Int = 0,
                params: Array<TypeSymbol> = arrayOf()
        ) = TypeSymbol(name, parentType, size, paramCount, params).also {
            builtin.add(it)
        }

        const val ptrSize = 64

        val Any = createBuiltinType("Any", null)
        val Primitive = TypeSymbol("Primitive", Any)

        val Integer = TypeSymbol("!I", Primitive)
        val UInteger = TypeSymbol("!U", Primitive)

        val Ptr = createBuiltinType("Ptr", Primitive, ptrSize, 1, arrayOf(Any))

        val I8 = createBuiltinType("I8", Integer, size = 8)
        //val U8 = TypeSymbol("U8", UInteger, size = 8)
        val I16 = createBuiltinType("I16", Integer, size = 16)
        //val U16 = TypeSymbol("U16", UInteger, size = 16)
        val I32 = createBuiltinType("I32", Integer, size = 32)
        //val U32 = TypeSymbol("U32", UInteger, size = 32)
        val I64 = createBuiltinType("I64", Integer, size = 64)
        //val U64 = TypeSymbol("U64", UInteger, size = 64)

        val Int = I32

        val Char = createBuiltinType("Char", I16, I16.size)

        val Float = createBuiltinType("Float", Primitive, 32)
        val Double = createBuiltinType("Double", Primitive, 64)

        val Bool = createBuiltinType("Bool", Primitive, size = 1)

        val Void = createBuiltinType("Void", Any, 0)

        val err = TypeSymbol("!Err", null, 0)

        val String get() = declared.find { it.path == "std.text.string.String" }!!
    }

    open fun isOfType(other: TypeSymbol): Boolean {
        if (this.path != other.path && parentType?.isOfType(other) != true) return false
        for (i in params.indices) {
            if (!params[i].isOfType(other.params[i])) return false
        }
        return true
    }

    fun commonType(other: TypeSymbol): TypeSymbol {
        return if (path == other.path) this else other.parentType?.commonType(this) ?: Any
    }

    operator fun invoke(params: Array<TypeSymbol>): TypeSymbol {
        if (params.size != paramCount) {
            throw BinderError("Wrong param count!")
        }
        for (i in params.indices) {
            if (!params[i].isOfType(this.params[i])) {
                throw BinderError("Parameters at position $i don't match!")
            }
        }
        return TypeSymbol(path, parentType, size, paramCount, params)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (javaClass != other.javaClass) return false

        other as TypeSymbol

        if (path != other.path) return false
        if (!params.contentEquals(other.params)) return false

        return true
    }

    override fun toString(): String {
        return if (paramCount == 0) path else path + '<' + params.joinToString(", ") { it.toString() } + '>'
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + params.contentHashCode()
        return result
    }
}