package mango.interpreter.binding.nodes.expressions

import mango.interpreter.symbols.TypeSymbol

class LiteralExpression(
    value: Any?,
    override val type: TypeSymbol
) : BoundExpression() {

    override val kind = Kind.LiteralExpression
    override fun toString() =
        if (type == TypeSymbol.String) { '"' + value.toString() + '"' }
        else { value.toString() }

    override val constantValue = BoundConstant(value)
    val value get() = constantValue.value
}