package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.Expression

class PointerAccessAssignment(
    val expression: Expression,
    val i: Expression,
    val value: Expression
) : Statement() {

    override val kind = Kind.PointerAccessAssignment
}
