package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.BoundExpression

class ExpressionStatement(
    val expression: BoundExpression
) : Statement() {
    override val kind = Kind.ExpressionStatement
}
