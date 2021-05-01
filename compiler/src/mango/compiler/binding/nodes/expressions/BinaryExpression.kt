package mango.compiler.binding.nodes.expressions

import mango.compiler.binding.ConstantFolding
import mango.compiler.binding.nodes.BiOperator

class BinaryExpression(
    val left: Expression,
    val operator: BiOperator,
    val right: Expression
) : Expression() {

    override val type get() = operator.resultType
    override val kind = Kind.BinaryExpression
    override val constantValue = ConstantFolding.computeConstant(left, operator, right)
}

