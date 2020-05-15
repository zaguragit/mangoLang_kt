package mango.binding

class BoundUnaryExpression(
    val operator: BoundUnaryOperator,
    val operand: BoundExpression
) : BoundExpression() {
    override val type: Type get() = operator.resultType
    override val boundType = BoundNodeType.UnaryExpression
    override val children get() = listOf(operand)
}

enum class BoundUnaryOperatorType {
    Identity,
    Negation,
    Not
}