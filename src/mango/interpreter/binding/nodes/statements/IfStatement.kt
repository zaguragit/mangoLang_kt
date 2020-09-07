package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.Expression

class IfStatement(
    val condition: Expression,
    val statement: BlockStatement,
    val elseStatement: Statement?
) : Statement() {

    override val kind = Kind.IfStatement
}
