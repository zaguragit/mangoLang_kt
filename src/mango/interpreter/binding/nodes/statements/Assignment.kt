package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.Expression

class Assignment(
    val assignee: Expression,
    val expression: Expression
) : Statement() {

    override val kind = Kind.AssignmentStatement
}
