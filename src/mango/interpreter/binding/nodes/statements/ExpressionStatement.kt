package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.Expression

class ExpressionStatement(
    val expression: Expression
) : Statement() {
    override val kind = Kind.ExpressionStatement
}
