package mango.symbols

class TypeSymbol private constructor(
    override val name: String
) : Symbol() {

    override val kind = Kind.Type

    companion object {

        val any = TypeSymbol("Any")

        val int = TypeSymbol("Int")
        val short = TypeSymbol("Short")
        val long = TypeSymbol("Long")
        val double = TypeSymbol("Double")
        val float = TypeSymbol("Float")

        val bool = TypeSymbol("Bool")

        val string = TypeSymbol("String")

        val unit = TypeSymbol("Unit")

        val error = TypeSymbol("?")
    }

    fun isOfType(other: TypeSymbol): Boolean {
        if (other == any) {
            return true
        }
        return this == other
    }
}