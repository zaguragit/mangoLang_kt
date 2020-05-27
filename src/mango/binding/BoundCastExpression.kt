package mango.binding

import mango.symbols.TypeSymbol

class BoundCastExpression(
    override val type: TypeSymbol,
    val expression: BoundExpression
) : BoundExpression() {
    override val boundType = BoundNodeType.CastExpression
}
