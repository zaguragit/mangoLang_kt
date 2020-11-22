package mango.interpreter.symbols

import mango.util.BinderError

open class TypeSymbol private constructor(
    override val name: String,
    val parentType: TypeSymbol?,
    val size: Int = -1,
    val paramCount: Int = 0,
    val params: Array<TypeSymbol> = arrayOf()
) : Symbol() {

    override val type get() = null!!

    override val kind = Kind.Type

    class StructTypeSymbol(
        name: String,
        val fields: Array<Field>,
        parentType: TypeSymbol
    ) : TypeSymbol(name, parentType, fields.sumBy { it.type.size }) {

        override val kind = Kind.StructType

        class Field(
            val name: String,
            val type: TypeSymbol,
            val isReadOnly: Boolean
        ) {
            override fun toString() = (if (isReadOnly) "val " else "var ") + name + ' ' + type
        }
    }

    class Fn(
        val returnType: TypeSymbol,
        val args: List<TypeSymbol>
    ) : TypeSymbol("Fn", Any, ptrSize) {

        override val kind = Kind.FunctionType
        override fun isOfType(other: TypeSymbol): Boolean = other is Fn || Any.isOfType(other)

        companion object {
            val entry = Fn(Unit, listOf())
        }
    }

    companion object {

        const val ptrSize = 64

        val map = HashMap<String, TypeSymbol>()

        val Any = TypeSymbol("Any", null)
        val Primitive = TypeSymbol("Primitive", Any)

        val Integer = TypeSymbol("!I", Primitive)
        val UInteger = TypeSymbol("!U", Primitive)

        val Ptr = TypeSymbol("Ptr", Primitive, ptrSize, 1, arrayOf(Any))

        val I8 = TypeSymbol("I8", Integer, size = 8)
        //val U8 = TypeSymbol("U8", UInteger, size = 8)
        val I16 = TypeSymbol("I16", Integer, size = 16)
        //val U16 = TypeSymbol("U16", UInteger, size = 16)
        val I32 = TypeSymbol("I32", Integer, size = 32)
        //val U32 = TypeSymbol("U32", UInteger, size = 32)
        val I64 = TypeSymbol("I64", Integer, size = 64)
        //val U64 = TypeSymbol("U64", UInteger, size = 64)

        val Int = I32

        val Float = TypeSymbol("Float", Primitive, 32)
        val Double = TypeSymbol("Double", Primitive, 64)

        val Bool = TypeSymbol("Bool", Primitive, size = 1)

        //val String = StructTypeSymbol("String", arrayOf(StructTypeSymbol.Field("length", Int), StructTypeSymbol.Field("chars", Ptr(arrayOf(I16)))), Any)

        val Unit = TypeSymbol("Unit", Any, 0)

        val err = TypeSymbol("!Err", null, 0)

        init {
            map["Any"] = Any
            map["Ptr"] = Ptr

            map["I8"] = I8
            map["I16"] = I16
            map["I32"] = I32
            map["I64"] = I64

            map["Int"] = I32

            map["Float"] = Float
            map["Double"] = Double
            map["Bool"] = Bool
            map["Unit"] = Unit
        }

        operator fun get(name: String) = map[name]
    }

    open fun isOfType(other: TypeSymbol): Boolean {
        if (this.name != other.name && parentType?.isOfType(other) != true) return false
        for (i in params.indices) {
            if (!params[i].isOfType(other.params[i])) return false
        }
        return true
    }

    fun commonType(other: TypeSymbol): TypeSymbol {
        return if (name == other.name) this else other.parentType?.commonType(this) ?: Any
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
        return TypeSymbol(name, parentType, size, paramCount, params)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (javaClass != other.javaClass) return false

        other as TypeSymbol

        if (name != other.name) return false
        if (!params.contentEquals(other.params)) return false

        return true
    }

    override fun toString(): String {
        return if (paramCount == 0) name else name + '<' + params.joinToString(", ") { it.toString() } + '>'
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + params.contentHashCode()
        return result
    }
}