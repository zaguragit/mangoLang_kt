package mango.interpreter.binding

import mango.interpreter.binding.nodes.BiOperator
import mango.interpreter.binding.nodes.UnOperator
import mango.interpreter.binding.nodes.expressions.BoundConstant
import mango.interpreter.binding.nodes.expressions.BoundExpression
import mango.interpreter.symbols.TypeSymbol

object ConstantFolding {

    fun computeConstant(
            left: BoundExpression,
            operator: BiOperator,
            right: BoundExpression
    ): BoundConstant? {
        val leftConst = left.constantValue
        val rightConst = right.constantValue
        if (leftConst == null) {
            return null
        }
        if (operator.type == BiOperator.Type.LogicAnd) {
            if (leftConst.value == false || rightConst?.value == false) {
                return BoundConstant(false)
            }
        }
        if (operator.type == BiOperator.Type.LogicOr) {
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
            BiOperator.Type.Add -> {
                if (left.type == TypeSymbol["String"]!!/*TypeSymbol.String*/) {
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
            operand: BoundExpression
    ): BoundConstant? {
        if (operand.constantValue != null) {
            val value = operand.constantValue!!.value
            return when (operator.type) {
                UnOperator.Type.Identity -> BoundConstant(value)
                UnOperator.Type.Negation -> BoundConstant(-(value as Int))
                UnOperator.Type.Not -> BoundConstant(!(value as Boolean))
            }
        }
        return null
    }
}