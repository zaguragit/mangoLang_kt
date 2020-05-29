package mango.interpreter.symbols

abstract class VariableSymbol(
    override val name: String,
    val type: TypeSymbol,
    val isReadOnly: Boolean
) : Symbol()