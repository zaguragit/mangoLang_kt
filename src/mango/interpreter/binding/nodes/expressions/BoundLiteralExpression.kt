package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.TypeSymbol

class BoundLiteralExpression(
    value: Any?,
    override val type: TypeSymbol/* = when (value) {
        is Byte -> TypeSymbol.I8
        is Short -> TypeSymbol.I16
        is Int -> TypeSymbol.I32
        is Long -> TypeSymbol.I64
        is Float -> TypeSymbol.Float
        is Double -> TypeSymbol.Double
        is Boolean -> TypeSymbol.Bool
        is String -> TypeSymbol.String
        else -> throw Exception("Unexpected literal of type ${value?.javaClass}")
    }*/
) : BoundExpression() {

    override val boundType = BoundNodeType.LiteralExpression
    override fun toString() =
        if (type == TypeSymbol.String) { '"' + value.toString() + '"' }
        else { value.toString() }

    override val constantValue = BoundConstant(value)
    val value get() = constantValue.value
}