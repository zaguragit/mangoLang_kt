package mango.binding

import mango.symbols.TypeSymbol

class BoundLiteralExpression(
    val value: Any?
) : BoundExpression() {

    override val type: TypeSymbol = when (value) {
        is Int -> TypeSymbol.int
        is Boolean -> TypeSymbol.bool
        is String -> TypeSymbol.string
        else -> throw Exception("Unexpected literal of type ${value?.javaClass}")
    }

    override val boundType = BoundNodeType.LiteralExpression
    override val children get() = listOf<BoundNode>()
    override fun toString() = value.toString()
    override fun getDataString() = value.toString()
}