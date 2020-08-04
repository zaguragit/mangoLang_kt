package mango.interpreter.binding

import mango.interpreter.binding.nodes.BoundBiOperator
import mango.interpreter.binding.nodes.BoundUnOperator
import mango.interpreter.binding.nodes.expressions.BoundConstant
import mango.interpreter.binding.nodes.expressions.BoundExpression
import mango.interpreter.symbols.TypeSymbol

object ConstantFolding {

    fun computeConstant(
        left: BoundExpression,
        operator: BoundBiOperator,
        right: BoundExpression
    ): BoundConstant? {
        val leftConst = left.constantValue
        val rightConst = right.constantValue
        if (leftConst == null) {
            return null
        }
        if (operator.type == BoundBiOperator.Type.LogicAnd) {
            if (leftConst.value == false || rightConst?.value == false) {
                return BoundConstant(false)
            }
        }
        if (operator.type == BoundBiOperator.Type.LogicOr) {
            if (leftConst.value == true || rightConst?.value == true) {
                return BoundConstant(true)
            }
        }
        if (rightConst == null) {
            return null
        }
        val leftVal = leftConst.value
        val rightVal = rightConst.value
        return BoundConstant(when (operator.type) {
            BoundBiOperator.Type.Add -> {
                if (left.type == TypeSymbol.String) {
                    leftVal as String + rightVal as String
                } else {
                    leftVal as Int + rightVal as Int
                }
            }
            BoundBiOperator.Type.Sub -> leftVal as Int - rightVal as Int
            BoundBiOperator.Type.Mul -> leftVal as Int * rightVal as Int
            BoundBiOperator.Type.Div -> leftVal as Int / rightVal as Int
            BoundBiOperator.Type.Rem -> leftVal as Int % rightVal as Int
            BoundBiOperator.Type.BitAnd -> {
                if (left.type == TypeSymbol.Bool) {
                    leftVal as Boolean and rightVal as Boolean
                } else {
                    leftVal as Int and rightVal as Int
                }
            }
            BoundBiOperator.Type.BitOr -> {
                if (left.type == TypeSymbol.Bool) {
                    leftVal as Boolean or rightVal as Boolean
                } else {
                    leftVal as Int or rightVal as Int
                }
            }
            BoundBiOperator.Type.LogicAnd -> leftVal as Boolean && rightVal as Boolean
            BoundBiOperator.Type.LogicOr -> leftVal as Boolean || rightVal as Boolean
            BoundBiOperator.Type.LessThan -> (leftVal as Int) < rightVal as Int
            BoundBiOperator.Type.MoreThan -> leftVal as Int > rightVal as Int
            BoundBiOperator.Type.IsEqual -> leftVal == rightVal
            BoundBiOperator.Type.IsEqualOrMore -> leftVal as Int >= rightVal as Int
            BoundBiOperator.Type.IsEqualOrLess -> leftVal as Int <= rightVal as Int
            BoundBiOperator.Type.IsNotEqual -> leftVal != rightVal
            BoundBiOperator.Type.IsIdentityEqual -> leftVal === rightVal
            BoundBiOperator.Type.IsNotIdentityEqual -> leftVal !== rightVal
        })
    }

    fun computeConstant(
            operator: BoundUnOperator,
            operand: BoundExpression
    ): BoundConstant? {
        if (operand.constantValue != null) {
            val value = operand.constantValue!!.value
            return when (operator.type) {
                BoundUnOperator.Type.Identity -> BoundConstant(value)
                BoundUnOperator.Type.Negation -> BoundConstant(-(value as Int))
                BoundUnOperator.Type.Not -> BoundConstant(!(value as Boolean))
            }
        }
        return null
    }
}