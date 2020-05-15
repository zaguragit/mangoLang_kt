package mango.binding

import mango.syntax.SyntaxType

class BoundBinaryOperator(
    val syntaxType: SyntaxType,
    val type: BoundBinaryOperatorType,
    val leftType: Type,
    val rightType: Type = leftType,
    val resultType: Type = leftType
) {
    companion object {

        private val operators = arrayOf(
                BoundBinaryOperator(SyntaxType.Plus, BoundBinaryOperatorType.Add, Type.Int),
                BoundBinaryOperator(SyntaxType.Minus, BoundBinaryOperatorType.Sub, Type.Int),
                BoundBinaryOperator(SyntaxType.Mul, BoundBinaryOperatorType.Mul, Type.Int),
                BoundBinaryOperator(SyntaxType.Div, BoundBinaryOperatorType.Div, Type.Int),
                BoundBinaryOperator(SyntaxType.Rem, BoundBinaryOperatorType.Rem, Type.Int),
                BoundBinaryOperator(SyntaxType.BitAnd, BoundBinaryOperatorType.BitAnd, Type.Int),
                BoundBinaryOperator(SyntaxType.BitOr, BoundBinaryOperatorType.BitOr, Type.Int),

                BoundBinaryOperator(SyntaxType.LessThan, BoundBinaryOperatorType.LessThan, Type.Int, resultType = Type.Bool),
                BoundBinaryOperator(SyntaxType.MoreThan, BoundBinaryOperatorType.MoreThan, Type.Int, resultType = Type.Bool),
                BoundBinaryOperator(SyntaxType.IsEqual, BoundBinaryOperatorType.IsEqual, Type.Int, resultType = Type.Bool),
                BoundBinaryOperator(SyntaxType.IsEqualOrLess, BoundBinaryOperatorType.IsEqualOrLess, Type.Int, resultType = Type.Bool),
                BoundBinaryOperator(SyntaxType.IsEqualOrMore, BoundBinaryOperatorType.IsEqualOrMore, Type.Int, resultType = Type.Bool),
                BoundBinaryOperator(SyntaxType.IsNotEqual, BoundBinaryOperatorType.IsNotEqual, Type.Int, resultType = Type.Bool),

                BoundBinaryOperator(SyntaxType.BitAnd, BoundBinaryOperatorType.BitAnd, Type.Bool),
                BoundBinaryOperator(SyntaxType.BitOr, BoundBinaryOperatorType.BitOr, Type.Bool),
                BoundBinaryOperator(SyntaxType.LogicAnd, BoundBinaryOperatorType.LogicAnd, Type.Bool),
                BoundBinaryOperator(SyntaxType.LogicOr, BoundBinaryOperatorType.LogicOr, Type.Bool),
                BoundBinaryOperator(SyntaxType.IsEqual, BoundBinaryOperatorType.IsEqual, Type.Bool),
                BoundBinaryOperator(SyntaxType.IsNotEqual, BoundBinaryOperatorType.IsNotEqual, Type.Bool))

        fun bind(syntaxType: SyntaxType, leftType: Type, rightType: Type): BoundBinaryOperator? {
            for (op in operators) {
                if (op.syntaxType == syntaxType &&
                    op.leftType == leftType &&
                    op.rightType == rightType) {
                    return op
                }
            }
            return null
        }
    }
}