package mango.interpreter.symbols


open class TypeSymbol private constructor(
    override val name: String,
    val parentType: TypeSymbol?,
    val paramCount: Int = 0,
    val params: Array<TypeSymbol> = arrayOf()
) : Symbol() {

    override val type = Type

    override val kind = Kind.Type

    init {
        map[name] = this
    }

    class StructTypeSymbol(
        name: String,
        val fields: Array<Field>
    ) : TypeSymbol(name, Any) {

        override val kind = Kind.Struct

        class Field(
            val name: String,
            val type: TypeSymbol
        )
    }

    companion object {

        val map = HashMap<String, TypeSymbol>()

        val Any = TypeSymbol("Any", null)

        val AnyI = TypeSymbol("!I", Any)
        val AnyU = TypeSymbol("!U", Any)

        val Ptr = TypeSymbol("Ptr", Any, 1, arrayOf(Any))

        val I8 = TypeSymbol("I8", AnyI)
        //val U8 = TypeSymbol("U8", AnyU)
        val I16 = TypeSymbol("I16", AnyI)
        //val U16 = TypeSymbol("U16", AnyU)
        val I32 = TypeSymbol("I32", AnyI)
        //val U32 = TypeSymbol("U32", AnyU)
        val I64 = TypeSymbol("I64", AnyI)
        //val U64 = TypeSymbol("U64", AnyU)

        val Int = I32

        init {
            map["Int"] = I32
        }
        //val Double = TypeSymbol("Double", any)
        //val Float = TypeSymbol("Float", any)

        val Bool = TypeSymbol("Bool", Any)

        val String = StructTypeSymbol("String", arrayOf(StructTypeSymbol.Field("length", Int), StructTypeSymbol.Field("chars", Ptr(arrayOf(I8)))))//TypeSymbol("String", Any)

        //val Fn = TypeSymbol("Fn", Any)

        val Unit = TypeSymbol("Unit", Any)

        val err = TypeSymbol("!Err", null)

        val Type = TypeSymbol("Type", Any)

        operator fun get(name: String) = map[name]
    }

    fun isOfType(other: TypeSymbol): Boolean = this == other || parentType?.isOfType(other) ?: false

    operator fun invoke(params: Array<TypeSymbol>): TypeSymbol {
        if (params.size != paramCount) {
            throw Exception("Wrong param count!")
        }
        for (i in params.indices) {
            if (!params[i].isOfType(this.params[i])) {
                throw Exception("Parameters at position $i don't match!")
            }
        }
        return TypeSymbol(name, parentType, paramCount, params)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

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