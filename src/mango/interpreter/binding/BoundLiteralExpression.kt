package mango.interpreter.binding

import mango.interpreter.symbols.TypeSymbol

class BoundLiteralExpression(
    value: Any?
) : BoundExpression() {

    override val type: TypeSymbol = when (value) {
        is Int -> TypeSymbol.int
        is Boolean -> TypeSymbol.bool
        is String -> TypeSymbol.string
        else -> throw Exception("Unexpected literal of type ${value?.javaClass}")
    }

    override val boundType = BoundNodeType.LiteralExpression
    override fun toString() =
        if (type == TypeSymbol.string) { '"' + value.toString() + '"' }
        else { value.toString() }

    override val constantValue = BoundConstant(value)
    val value get() = constantValue.value
}