package mango.interpreter.binding

import mango.interpreter.symbols.VariableSymbol

class BoundAssignmentExpression(
        val variable: VariableSymbol,
        val expression: BoundExpression
) : BoundExpression() {

    override val type get() = expression.type
    override val boundType = BoundNodeType.AssignmentExpression
}
