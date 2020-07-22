package mango.interpreter.symbols


open class TypeSymbol private constructor(
    override val name: String,
    val parentType: TypeSymbol?,
    val paramCount: Int = 0,
    val params: Array<TypeSymbol> = arrayOf(),
    val isBuiltin: Boolean = false
) : Symbol() {

    override val type = Type

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

    companion object {

        val map = HashMap<String, TypeSymbol>()

        val Any = TypeSymbol("Any", null, isBuiltin = true)

        val AnyI = TypeSymbol("!I", Any, isBuiltin = true)
        val AnyU = TypeSymbol("!U", Any, isBuiltin = true)

        val Ptr = TypeSymbol("Ptr", Any, 1, arrayOf(Any), isBuiltin = true)

        val I8 = TypeSymbol("I8", AnyI, isBuiltin = true)
        //val U8 = TypeSymbol("U8", AnyU)
        val I16 = TypeSymbol("I16", AnyI, isBuiltin = true)
        //val U16 = TypeSymbol("U16", AnyU)
        val I32 = TypeSymbol("I32", AnyI, isBuiltin = true)
        //val U32 = TypeSymbol("U32", AnyU)
        val I64 = TypeSymbol("I64", AnyI, isBuiltin = true)
        //val U64 = TypeSymbol("U64", AnyU)

        val Int = I32

        init {
            map["Int"] = I32
        }
        //val Double = TypeSymbol("Double", any)
        //val Float = TypeSymbol("Float", any)

        val Bool = TypeSymbol("Bool", Any, isBuiltin = true)

        val String = StructTypeSymbol("String", arrayOf(StructTypeSymbol.Field("length", Int), StructTypeSymbol.Field("chars", Ptr(arrayOf(I8)))), Any)

        //val Fn = TypeSymbol("Fn", Any)

        val Unit = TypeSymbol("Unit", Any, isBuiltin = true)

        val err = TypeSymbol("!Err", null, isBuiltin = true)

        val Type = TypeSymbol("Type", Any, isBuiltin = true) // StructTypeSymbol("Type", arrayOf(StructTypeSymbol.Field("name", String), StructTypeSymbol.Field("parentType", Ptr(arrayOf(Type)))), Any)

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