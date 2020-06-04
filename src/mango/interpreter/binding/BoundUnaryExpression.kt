package mango.interpreter.binding

class BoundUnaryExpression(
    val operator: BoundUnaryOperator,
    val operand: BoundExpression
) : BoundExpression() {

    override val type = operator.resultType
    override val boundType = BoundNodeType.UnaryExpression
    override val constantValue = ConstantFolding.computeConstant(operator, operand)
}

