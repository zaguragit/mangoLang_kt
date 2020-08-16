package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType

class BoundPointerAccess(
    val expression: BoundExpression,
    val i: BoundExpression
) : BoundExpression() {

    override val type = expression.type.params[0]
    override val kind = BoundNodeType.PointerAccessExpression
}
