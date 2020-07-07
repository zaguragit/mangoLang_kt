package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.nodes.expressions.BoundExpression

class BoundIfStatement(
        val condition: BoundExpression,
        val statement: BoundBlockStatement,
        val elseStatement: BoundStatement?
) : BoundStatement() {

    override val boundType = BoundNodeType.IfStatement
}
