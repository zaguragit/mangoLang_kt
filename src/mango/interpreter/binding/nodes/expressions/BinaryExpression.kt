package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BiOperator
import mango.interpreter.binding.ConstantFolding

class BinaryExpression(
        val left: BoundExpression,
        val operator: BiOperator,
        val right: BoundExpression
) : BoundExpression() {

    override val type get() = operator.resultType
    override val kind = Kind.BinaryExpression
    override val constantValue = ConstantFolding.computeConstant(left, operator, right)
}

