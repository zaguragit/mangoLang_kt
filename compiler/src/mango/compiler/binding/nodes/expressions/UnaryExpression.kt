package mango.compiler.binding.nodes.expressions

import mango.compiler.binding.ConstantFolding
import mango.compiler.binding.nodes.UnOperator

class UnaryExpression(
    val operator: UnOperator,
    val operand: Expression
) : Expression() {

    override val type = operator.resultType
    override val kind = Kind.UnaryExpression
    override val constantValue = ConstantFolding.computeConstant(operator, operand)
}

