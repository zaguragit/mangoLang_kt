package mango.symbols

class FunctionSymbol(
    override val name: String,
    val parameters: Array<ParameterSymbol>,
    val type: TypeSymbol
) : Symbol() {
    override val kind = Kind.Function
}