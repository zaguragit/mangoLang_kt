package mango.symbols


class TypeSymbol private constructor(
    override val name: String,
    val parentType: TypeSymbol?
) : Symbol() {

    override val kind = Kind.Type

    companion object {

        val any = TypeSymbol("Any", null)

        val int = TypeSymbol("Int", any)
        val short = TypeSymbol("Short", any)
        val long = TypeSymbol("Long", any)
        val double = TypeSymbol("Double", any)
        val float = TypeSymbol("Float", any)

        val bool = TypeSymbol("Bool", any)

        val string = TypeSymbol("String", any)

        val unit = TypeSymbol("Unit", any)

        val unknown = TypeSymbol("?Unknown?", any)
        val error = TypeSymbol("?Error?", any)

        fun lookup(name: String) = when (name) {
            "Any" -> any
            "Int" -> int
            "Short" -> short
            "Long" -> long
            "Double" -> double
            "Float" -> float
            "Bool" -> bool
            "String" -> string
            "Unit" -> unit
            else -> null
        }
    }

    fun isOfType(other: TypeSymbol): Boolean = this == other || parentType?.isOfType(other) ?: false
}