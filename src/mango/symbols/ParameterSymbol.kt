package mango.symbols

class ParameterSymbol(
    name: String,
    type: TypeSymbol
) : VariableSymbol(
    name,
    type,
    true
) {
    override val kind = Kind.Parameter
}