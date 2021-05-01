package mango.compiler.binding.nodes.expressions

import mango.compiler.symbols.TypeSymbol

class CastExpression(
    override val type: TypeSymbol,
    val expression: Expression
) : Expression() {
    override val kind = Kind.CastExpression
}
