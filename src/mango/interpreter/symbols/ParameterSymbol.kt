package mango.interpreter.symbols

class ParameterSymbol(
    name: String,
    type: TypeSymbol
) : LocalVariableSymbol(
    name,
    type,
    true,
    null
) {
    override val kind = Kind.Parameter
}