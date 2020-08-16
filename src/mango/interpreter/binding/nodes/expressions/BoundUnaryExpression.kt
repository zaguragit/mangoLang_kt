package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.nodes.BoundUnOperator
import mango.interpreter.binding.ConstantFolding

class BoundUnaryExpression(
        val operator: BoundUnOperator,
        val operand: BoundExpression
) : BoundExpression() {

    override val type = operator.resultType
    override val kind = BoundNodeType.UnaryExpression
    override val constantValue = ConstantFolding.computeConstant(operator, operand)
}

