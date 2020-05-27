package mango.binding

import mango.symbols.VariableSymbol

class BoundVariableExpression(
    val variable: VariableSymbol
) : BoundExpression() {

    override val type = variable.type
    override val boundType = BoundNodeType.VariableExpression

    override fun toString() = variable.name
}
