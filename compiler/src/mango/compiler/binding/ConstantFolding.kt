package mango.compiler.binding

import mango.compiler.binding.nodes.BiOperator
import mango.compiler.binding.nodes.UnOperator
import mango.compiler.binding.nodes.expressions.BoundConstant
import mango.compiler.binding.nodes.expressions.Expression
import mango.compiler.symbols.TypeSymbol

object ConstantFolding {

    fun computeConstant(
        left: Expression,
        operator: BiOperator,
        right: Expression
    ): BoundConstant? {
        val leftConst = left.constantValue ?: return null
        val rightConst = right.constantValue ?: return null
        val leftVal = leftConst.value
        val rightVal = rightConst.value
        return BoundConstant(when (operator.type) {
            BiOperator.Type.Add -> {
                if (left.type == TypeSymbol.String) {
                    leftVal as String + rightVal as String
                } else {
                    leftVal as Int + rightVal as Int
                }
            }
            BiOperator.Type.Sub -> leftVal as Int - rightVal as Int
            BiOperator.Type.Mul -> leftVal as Int * rightVal as Int
            BiOperator.Type.Div -> leftVal as Int / rightVal as Int
            BiOperator.Type.Rem -> leftVal as Int % rightVal as Int
            BiOperator.Type.BitAnd -> {
                if (left.type == TypeSymbol.Bool) {
                    leftVal as Boolean and rightVal as Boolean
                } else {
                    leftVal as Int and rightVal as Int
                }
            }
            BiOperator.Type.BitOr -> {
                if (left.type == TypeSymbol.Bool) {
                    leftVal as Boolean or rightVal as Boolean
                } else {
                    leftVal as Int or rightVal as Int
                }
            }
            BiOperator.Type.LogicAnd -> leftVal as Boolean && rightVal as Boolean
            BiOperator.Type.LogicOr -> leftVal as Boolean || rightVal as Boolean
            BiOperator.Type.LessThan -> (leftVal as Int) < rightVal as Int
            BiOperator.Type.MoreThan -> leftVal as Int > rightVal as Int
            BiOperator.Type.IsEqual -> leftVal == rightVal
            BiOperator.Type.IsEqualOrMore -> leftVal as Int >= rightVal as Int
            BiOperator.Type.IsEqualOrLess -> leftVal as Int <= rightVal as Int
            BiOperator.Type.IsNotEqual -> leftVal != rightVal
            BiOperator.Type.IsIdentityEqual -> leftVal === rightVal
            BiOperator.Type.IsNotIdentityEqual -> leftVal !== rightVal
        })
    }

    fun computeConstant(
        operator: UnOperator,
        operand: Expression
    ): BoundConstant? {
        val c = operand.constantValue ?: return null
        val value = c.value
        return BoundConstant(when (operator.type) {
            UnOperator.Type.Identity -> value
            UnOperator.Type.Negation -> -(value as Int)
            UnOperator.Type.Not -> !(value as Boolean)
        })
    }
}