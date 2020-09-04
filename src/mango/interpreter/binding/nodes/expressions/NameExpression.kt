package mango.interpreter.binding.nodes.expressions

import mango.interpreter.symbols.VariableSymbol

class NameExpression(
    val symbol: VariableSymbol
) : BoundExpression() {

    override val type = symbol.type
    override val kind = Kind.VariableExpression

    override fun toString() = symbol.name

    override val constantValue get() = symbol.constant
}
