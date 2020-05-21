package mango.symbols

class ParameterSymbol(
    name: String,
    type: TypeSymbol
) : LocalVariableSymbol(
    name,
    type,
    true
) {
    override val kind = Kind.Parameter
}