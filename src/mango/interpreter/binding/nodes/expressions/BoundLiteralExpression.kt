package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.TypeSymbol

class BoundLiteralExpression(
    value: Any?
) : BoundExpression() {

    override val type: TypeSymbol = when (value) {
        is Int -> TypeSymbol.Int
        is Boolean -> TypeSymbol.Bool
        is String -> TypeSymbol.String
        else -> throw Exception("Unexpected literal of type ${value?.javaClass}")
    }

    override val boundType = BoundNodeType.LiteralExpression
    override fun toString() =
        if (type == TypeSymbol.String) { '"' + value.toString() + '"' }
        else { value.toString() }

    override val constantValue = BoundConstant(value)
    val value get() = constantValue.value
}