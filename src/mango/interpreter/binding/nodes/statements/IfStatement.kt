package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.BoundExpression

class IfStatement(
        val condition: BoundExpression,
        val statement: BlockStatement,
        val elseStatement: Statement?
) : Statement() {

    override val kind = Kind.IfStatement
}
