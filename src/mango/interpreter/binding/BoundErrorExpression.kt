package mango.interpreter.binding

import mango.interpreter.symbols.TypeSymbol

class BoundErrorExpression : BoundExpression() {
    override val type = TypeSymbol.error
    override val boundType = BoundNodeType.ErrorExpression
}