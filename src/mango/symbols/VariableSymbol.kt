package mango.symbols

open class VariableSymbol(
    override val name: String,
    val type: TypeSymbol,
    val isReadOnly: Boolean
) : Symbol() {
    override val kind = Kind.Variable
}