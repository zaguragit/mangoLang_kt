package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundBiOperator
import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.ConstantFolding

class BoundBinaryExpression(
    val left: BoundExpression,
    val operator: BoundBiOperator,
    val right: BoundExpression
) : BoundExpression() {

    override val type get() = operator.resultType
    override val boundType = BoundNodeType.BinaryExpression
    override val constantValue = ConstantFolding.computeConstant(left, operator, right)
}

