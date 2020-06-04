package mango.interpreter.binding

class BoundBinaryExpression(
    val left: BoundExpression,
    val operator: BoundBinaryOperator,
    val right: BoundExpression
) : BoundExpression() {

    override val type get() = operator.resultType
    override val boundType = BoundNodeType.BinaryExpression
    override val constantValue = ConstantFolding.computeConstant(left, operator, right)
}

