package mango.compiler.binding.nodes.statements

import mango.compiler.binding.nodes.expressions.Expression

class ReturnStatement(
    val expression: Expression?
) : Statement() {
    override val kind = Kind.ReturnStatement
}