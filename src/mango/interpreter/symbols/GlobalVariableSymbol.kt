package mango.interpreter.symbols

import mango.interpreter.binding.BoundConstant

open class GlobalVariableSymbol(
    name: String,
    type: TypeSymbol,
    isReadOnly: Boolean,
    constant: BoundConstant?
) : VariableSymbol(
    name,
    type,
    isReadOnly,
    constant
) {
    override val kind = Kind.GlobalVariable
}