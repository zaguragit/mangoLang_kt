package mango.interpreter.symbols

import mango.util.BinderError


open class TypeSymbol private constructor(
    override val name: String,
    val parentType: TypeSymbol?,
    val paramCount: Int = 0,
    val params: Array<TypeSymbol> = arrayOf(),
    val size: Int = -1
) : Symbol() {

    override val type get() = null!!

    override val kind = Kind.Type

    init {
        map[name] = this
    }

    class StructTypeSymbol(
        name: String,
        val fields: Array<Field>,
        parentType: TypeSymbol
    ) : TypeSymbol(name, parentType) {

        override val kind = Kind.Struct

        class Field(
            val name: String,
            val type: TypeSymbol
        )
    }

    class Fn(
        val returnType: TypeSymbol,
        val args: Collection<TypeSymbol>
    ) : TypeSymbol("Fn", Any) {

        override val kind = Kind.FunctionType
        override fun isOfType(other: TypeSymbol): Boolean = other is Fn || Any.isOfType(other)

        companion object {
            val entry = Fn(Unit, listOf())
        }
    }

    companion object {

        val map = HashMap<String, TypeSymbol>()

        val Any = TypeSymbol("Any", null)
        val Primitive = TypeSymbol("Primitive", Any)

        val Integer = TypeSymbol("!I", Primitive)
        val UInteger = TypeSymbol("!U", Primitive)

        val Ptr = TypeSymbol("Ptr", Primitive, 1, arrayOf(Any))

        val I8 = TypeSymbol("I8", Integer, size = 8)
        //val U8 = TypeSymbol("U8", AnyU)
        val I16 = TypeSymbol("I16", Integer, size = 16)
        //val U16 = TypeSymbol("U16", AnyU)
        val I32 = TypeSymbol("I32", Integer, size = 32)
        //val U32 = TypeSymbol("U32", AnyU)
        val I64 = TypeSymbol("I64", Integer, size = 64)
        //val U64 = TypeSymbol("U64", AnyU)

        val Int = I32

        init {
            map["Int"] = I32
        }

        val Float = TypeSymbol("Float", Primitive)
        val Double = TypeSymbol("Double", Primitive)

        val Bool = TypeSymbol("Bool", Primitive, size = 1)

        val String = StructTypeSymbol("String", arrayOf(StructTypeSymbol.Field("length", Int), StructTypeSymbol.Field("chars", Ptr(arrayOf(I8)))), Any)

        val Unit = TypeSymbol("Unit", Any)

        val err = TypeSymbol("!Err", null)

        //val Array = StructTypeSymbol("String", arrayOf(StructTypeSymbol.Field("length", Int), StructTypeSymbol.Field("chars", Ptr(arrayOf(I8)))), Any)

        operator fun get(name: String) = map[name]
    }

    open fun isOfType(other: TypeSymbol): Boolean = this.name == other.name || parentType?.isOfType(other) ?: false

    operator fun invoke(params: Array<TypeSymbol>): TypeSymbol {
        if (params.size != paramCount) {
            throw BinderError("Wrong param count!")
        }
        for (i in params.indices) {
            if (!params[i].isOfType(this.params[i])) {
                throw BinderError("Parameters at position $i don't match!")
            }
        }
        return TypeSymbol(name, parentType, paramCount, params)
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

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + params.contentHashCode()
        return result
    }
}