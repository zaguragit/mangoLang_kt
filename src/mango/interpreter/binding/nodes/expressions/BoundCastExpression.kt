package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.TypeSymbol

class BoundCastExpression(
    override val type: TypeSymbol,
    val expression: BoundExpression
) : BoundExpression() {
    override val boundType = BoundNodeType.CastExpression
}
