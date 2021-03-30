package mango.compiler.binding.nodes.statements

import mango.compiler.binding.nodes.expressions.Expression

class Assignment(
    val assignee: Expression,
    val expression: Expression
) : Statement() {

    override val kind = Kind.AssignmentStatement
}
