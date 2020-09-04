package mango.interpreter.binding.nodes.expressions

import mango.interpreter.symbols.TypeSymbol

class CastExpression(
    override val type: TypeSymbol,
    val expression: BoundExpression
) : BoundExpression() {
    override val kind = Kind.CastExpression
}
