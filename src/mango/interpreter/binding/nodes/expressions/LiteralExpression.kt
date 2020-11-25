package mango.interpreter.binding.nodes.expressions

import mango.interpreter.symbols.TypeSymbol

class LiteralExpression(
    value: Any?,
    override val type: TypeSymbol
) : Expression() {

    override val kind = Kind.LiteralExpression
    override fun toString() =
        if (type == TypeSymbol["String"]!!/*TypeSymbol.String*/) { '"' + value.toString() + '"' }
        else { value.toString() }

    override val constantValue = BoundConstant(value)
    val value get() = constantValue.value

    companion object {
        fun nullEquivalent(type: TypeSymbol): LiteralExpression {
            return LiteralExpression(when {
                type.isOfType(TypeSymbol.Integer) -> 0
                type.isOfType(TypeSymbol.UInteger) -> 0
                type.isOfType(TypeSymbol.Float) -> 0f
                type.isOfType(TypeSymbol.Double) -> 0.0
                type.isOfType(TypeSymbol.Bool) -> false
                else -> null
            }, type)
        }
    }
}