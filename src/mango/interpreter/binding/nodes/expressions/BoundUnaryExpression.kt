package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.nodes.BoundUnaryOperator
import mango.interpreter.binding.ConstantFolding

class BoundUnaryExpression(
        val operator: BoundUnaryOperator,
        val operand: BoundExpression
) : BoundExpression() {

    override val type = operator.resultType
    override val boundType = BoundNodeType.UnaryExpression
    override val constantValue = ConstantFolding.computeConstant(operator, operand)
}

