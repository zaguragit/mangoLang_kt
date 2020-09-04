package mango.interpreter.binding.nodes.expressions

class PointerAccess(
    val expression: BoundExpression,
    val i: BoundExpression
) : BoundExpression() {

    override val type = expression.type.params[0]
    override val kind = Kind.PointerAccessExpression
}
