package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.VariableSymbol

class BoundVariableExpression(
    val symbol: VariableSymbol
) : BoundExpression() {

    override val type = symbol.type
    override val boundType = BoundNodeType.VariableExpression

    override fun toString() = symbol.name

    override val constantValue get() = symbol.constant
}
