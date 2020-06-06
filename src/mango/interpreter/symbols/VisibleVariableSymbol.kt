package mango.interpreter.symbols

import mango.interpreter.binding.BoundConstant

open class VisibleVariableSymbol(
    name: String,
    type: TypeSymbol,
    isReadOnly: Boolean,
    constant: BoundConstant?,
    override val path: String
) : VariableSymbol(
    name,
    type,
    isReadOnly,
    constant
), VisibleSymbol {
    override val kind = Kind.GlobalVariable
}