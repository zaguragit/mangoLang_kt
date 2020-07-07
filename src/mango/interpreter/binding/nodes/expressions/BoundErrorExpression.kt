package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.TypeSymbol

class BoundErrorExpression : BoundExpression() {
    override val type = TypeSymbol.err
    override val boundType = BoundNodeType.ErrorExpression
}