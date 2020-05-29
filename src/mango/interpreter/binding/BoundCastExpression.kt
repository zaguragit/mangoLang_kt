package mango.interpreter.binding

import mango.interpreter.symbols.TypeSymbol

class BoundCastExpression(
    override val type: TypeSymbol,
    val expression: BoundExpression
) : BoundExpression() {
    override val boundType = BoundNodeType.CastExpression
}
