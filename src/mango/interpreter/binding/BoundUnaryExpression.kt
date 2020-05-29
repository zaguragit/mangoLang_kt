package mango.interpreter.binding

class BoundUnaryExpression(
    val operator: BoundUnaryOperator,
    val operand: BoundExpression
) : BoundExpression() {
    override val type = operator.resultType
    override val boundType = BoundNodeType.UnaryExpression
}

enum class BoundUnaryOperatorType {
    Identity,
    Negation,
    Not
}