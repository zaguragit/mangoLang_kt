package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.Expression

class ReturnStatement(
    val expression: Expression?
) : Statement() {
    override val kind = Kind.ReturnStatement
}