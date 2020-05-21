package mango.symbols

open class LocalVariableSymbol(
    name: String,
    type: TypeSymbol,
    isReadOnly: Boolean
) : VariableSymbol(
    name,
    type,
    isReadOnly
) {
    override val kind = Kind.LocalVariable
}