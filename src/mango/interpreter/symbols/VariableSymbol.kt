package mango.interpreter.symbols

import mango.interpreter.binding.BoundConstant

abstract class VariableSymbol(
    override val name: String,
    val type: TypeSymbol,
    val isReadOnly: Boolean,
    constant: BoundConstant?
) : Symbol() {

    val constant =
        if (isReadOnly) { constant }
        else { null }
}