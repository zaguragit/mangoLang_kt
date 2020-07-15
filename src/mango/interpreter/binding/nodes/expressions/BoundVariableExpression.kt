package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.Symbol
import mango.interpreter.symbols.VariableSymbol

class BoundVariableExpression(
    val symbol: Symbol
) : BoundExpression() {

    override val type = symbol.type
    override val boundType = BoundNodeType.VariableExpression

    override fun toString() = symbol.name

    override val constantValue get() = if (symbol is VariableSymbol) symbol.constant else null
}
