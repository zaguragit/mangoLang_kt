package mango.binding
class BoundLiteralExpression(
        val value: Any?,
        override val type: Type
) : BoundExpression() {
    override val boundType = BoundNodeType.LiteralExpression
    override val children get() = listOf<BoundNode>()
    override fun toString() = value.toString()
    override fun getDataString() = value.toString()
}