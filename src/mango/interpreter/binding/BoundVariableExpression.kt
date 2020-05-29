package mango.interpreter.binding

import mango.interpreter.symbols.VariableSymbol

class BoundVariableExpression(
    val variable: VariableSymbol
) : BoundExpression() {

    override val type = variable.type
    override val boundType = BoundNodeType.VariableExpression

    override fun toString() = variable.name
}
