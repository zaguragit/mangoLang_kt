package mango.symbols

open class GlobalVariableSymbol(
    name: String,
    type: TypeSymbol,
    isReadOnly: Boolean
) : VariableSymbol(
    name,
    type,
    isReadOnly
) {
    override val kind = Kind.GlobalVariable
}