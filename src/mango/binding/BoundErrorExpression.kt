package mango.binding

import mango.symbols.TypeSymbol

class BoundErrorExpression : BoundExpression() {
    override val type = TypeSymbol.error
    override val boundType = BoundNodeType.ErrorExpression
}