package mango.interpreter.binding

import mango.interpreter.symbols.TypeSymbol

object ConstantFolding {

    fun computeConstant(
        left: BoundExpression,
        operator: BoundBinaryOperator,
        right: BoundExpression
    ): BoundConstant? {
        val leftConst = left.constantValue
        val rightConst = right.constantValue
        if (leftConst == null) {
            return null
        }
        if (operator.type == BoundBinaryOperatorType.LogicAnd) {
            if (leftConst.value == false || rightConst?.value == false) {
                return BoundConstant(false)
            }
        }
        if (operator.type == BoundBinaryOperatorType.LogicOr) {
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
            BoundBinaryOperatorType.Add -> {
                if (left.type == TypeSymbol.string) {
                    leftVal as String + rightVal as String
                }
                else {
                    leftVal as Int + rightVal as Int
                }
            }
            BoundBinaryOperatorType.Sub -> leftVal as Int - rightVal as Int
            BoundBinaryOperatorType.Mul -> leftVal as Int * rightVal as Int
            BoundBinaryOperatorType.Div -> leftVal as Int / rightVal as Int
            BoundBinaryOperatorType.Rem -> leftVal as Int % rightVal as Int
            BoundBinaryOperatorType.BitAnd -> {
                if (left.type == TypeSymbol.bool) {
                    leftVal as Boolean and rightVal as Boolean
                }
                else {
                    leftVal as Int and rightVal as Int
                }
            }
            BoundBinaryOperatorType.BitOr -> {
                if (left.type == TypeSymbol.bool) {
                    leftVal as Boolean or rightVal as Boolean
                }
                else {
                    leftVal as Int or rightVal as Int
                }
            }
            BoundBinaryOperatorType.LogicAnd -> leftVal as Boolean && rightVal as Boolean
            BoundBinaryOperatorType.LogicOr -> leftVal as Boolean || rightVal as Boolean
            BoundBinaryOperatorType.LessThan -> (leftVal as Int) < rightVal as Int
            BoundBinaryOperatorType.MoreThan -> leftVal as Int > rightVal as Int
            BoundBinaryOperatorType.IsEqual -> leftVal == rightVal
            BoundBinaryOperatorType.IsEqualOrMore -> leftVal as Int >= rightVal as Int
            BoundBinaryOperatorType.IsEqualOrLess -> leftVal as Int <= rightVal as Int
            BoundBinaryOperatorType.IsNotEqual -> leftVal != rightVal
            BoundBinaryOperatorType.IsIdentityEqual -> leftVal === rightVal
            BoundBinaryOperatorType.IsNotIdentityEqual -> leftVal !== rightVal
        })
    }

    fun computeConstant(
        operator: BoundUnaryOperator,
        operand: BoundExpression
    ): BoundConstant? {
        if (operand.constantValue != null) {
            val value = operand.constantValue!!.value
            return when (operator.type) {
                BoundUnaryOperatorType.Identity -> BoundConstant(value)
                BoundUnaryOperatorType.Negation -> BoundConstant(-(value as Int))
                BoundUnaryOperatorType.Not -> BoundConstant(!(value as Boolean))
            }
        }
        return null
    }
}