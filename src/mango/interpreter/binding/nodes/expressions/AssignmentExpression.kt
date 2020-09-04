package mango.interpreter.binding.nodes.expressions

import mango.interpreter.symbols.VariableSymbol

class AssignmentExpression(
    val variable: VariableSymbol,
    val expression: BoundExpression
) : BoundExpression() {

    override val type get() = expression.type
    override val kind = Kind.AssignmentExpression
}
