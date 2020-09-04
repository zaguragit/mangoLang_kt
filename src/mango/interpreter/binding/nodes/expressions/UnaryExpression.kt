package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.UnOperator
import mango.interpreter.binding.ConstantFolding

class UnaryExpression(
        val operator: UnOperator,
        val operand: BoundExpression
) : BoundExpression() {

    override val type = operator.resultType
    override val kind = Kind.UnaryExpression
    override val constantValue = ConstantFolding.computeConstant(operator, operand)
}

