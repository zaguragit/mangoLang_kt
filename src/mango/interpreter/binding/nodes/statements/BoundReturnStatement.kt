package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.nodes.expressions.BoundExpression

class BoundReturnStatement(
    val expression: BoundExpression?
) : BoundStatement() {
    override val boundType = BoundNodeType.ReturnStatement
}