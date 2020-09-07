package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.ConstantFolding
import mango.interpreter.binding.nodes.UnOperator

class UnaryExpression(
        val operator: UnOperator,
        val operand: Expression
) : Expression() {

    override val type = operator.resultType
    override val kind = Kind.UnaryExpression
    override val constantValue = ConstantFolding.computeConstant(operator, operand)
}

