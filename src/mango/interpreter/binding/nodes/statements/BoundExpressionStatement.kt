package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.nodes.expressions.BoundExpression

class BoundExpressionStatement(
    val expression: BoundExpression
) : BoundStatement() {
    override val kind = BoundNodeType.ExpressionStatement
}
