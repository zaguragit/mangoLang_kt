package mango.interpreter.binding.nodes

import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.syntax.SyntaxType

class BoundBinaryOperator(
        val syntaxType: SyntaxType,
        val type: BoundBinaryOperatorType,
        val leftType: TypeSymbol,
        val rightType: TypeSymbol = leftType,
        val resultType: TypeSymbol = leftType
) {
    companion object {

        private val operators = arrayOf(
                BoundBinaryOperator(SyntaxType.Plus, BoundBinaryOperatorType.Add, TypeSymbol.AnyI),
                BoundBinaryOperator(SyntaxType.Minus, BoundBinaryOperatorType.Sub, TypeSymbol.AnyI),
                BoundBinaryOperator(SyntaxType.Star, BoundBinaryOperatorType.Mul, TypeSymbol.AnyI),
                BoundBinaryOperator(SyntaxType.Div, BoundBinaryOperatorType.Div, TypeSymbol.AnyI),
                BoundBinaryOperator(SyntaxType.Rem, BoundBinaryOperatorType.Rem, TypeSymbol.AnyI),
                BoundBinaryOperator(SyntaxType.BitAnd, BoundBinaryOperatorType.BitAnd, TypeSymbol.AnyI),
                BoundBinaryOperator(SyntaxType.BitOr, BoundBinaryOperatorType.BitOr, TypeSymbol.AnyI),

                BoundBinaryOperator(SyntaxType.LessThan, BoundBinaryOperatorType.LessThan, TypeSymbol.AnyI, resultType = TypeSymbol.Bool),
                BoundBinaryOperator(SyntaxType.MoreThan, BoundBinaryOperatorType.MoreThan, TypeSymbol.AnyI, resultType = TypeSymbol.Bool),
                BoundBinaryOperator(SyntaxType.IsEqualOrLess, BoundBinaryOperatorType.IsEqualOrLess, TypeSymbol.AnyI, resultType = TypeSymbol.Bool),
                BoundBinaryOperator(SyntaxType.IsEqualOrMore, BoundBinaryOperatorType.IsEqualOrMore, TypeSymbol.AnyI, resultType = TypeSymbol.Bool),

                BoundBinaryOperator(SyntaxType.IsEqual, BoundBinaryOperatorType.IsEqual, TypeSymbol.Primitive, resultType = TypeSymbol.Bool),
                BoundBinaryOperator(SyntaxType.IsNotEqual, BoundBinaryOperatorType.IsNotEqual, TypeSymbol.Primitive, resultType = TypeSymbol.Bool),

                BoundBinaryOperator(SyntaxType.IsIdentityEqual, BoundBinaryOperatorType.IsIdentityEqual, TypeSymbol.Any, resultType = TypeSymbol.Bool),
                BoundBinaryOperator(SyntaxType.IsNotIdentityEqual, BoundBinaryOperatorType.IsNotIdentityEqual, TypeSymbol.Any, resultType = TypeSymbol.Bool),

                BoundBinaryOperator(SyntaxType.BitAnd, BoundBinaryOperatorType.BitAnd, TypeSymbol.Bool),
                BoundBinaryOperator(SyntaxType.BitOr, BoundBinaryOperatorType.BitOr, TypeSymbol.Bool),
                BoundBinaryOperator(SyntaxType.LogicAnd, BoundBinaryOperatorType.LogicAnd, TypeSymbol.Bool),
                BoundBinaryOperator(SyntaxType.LogicOr, BoundBinaryOperatorType.LogicOr, TypeSymbol.Bool),

                BoundBinaryOperator(SyntaxType.Plus, BoundBinaryOperatorType.Add, TypeSymbol.String))

        fun bind(syntaxType: SyntaxType, leftType: TypeSymbol, rightType: TypeSymbol): BoundBinaryOperator? {
            if (syntaxType == SyntaxType.IsEqual ||
                syntaxType == SyntaxType.IsNotEqual ||
                syntaxType == SyntaxType.IsIdentityEqual ||
                syntaxType == SyntaxType.IsNotIdentityEqual) {
                if (!leftType.isOfType(rightType) && !rightType.isOfType(leftType)) {
                    return null
                }
            }
            for (op in operators) {
                if (op.syntaxType == syntaxType &&
                    leftType.isOfType(op.leftType) &&
                    rightType.isOfType(op.rightType)) {
                    return op
                }
            }
            return null
        }


        fun getString(type: BoundBinaryOperatorType): String = when (type) {
            BoundBinaryOperatorType.Add -> "+"
            BoundBinaryOperatorType.Sub -> "-"
            BoundBinaryOperatorType.Mul -> "*"
            BoundBinaryOperatorType.Div -> "/"
            BoundBinaryOperatorType.Rem -> "%"
            BoundBinaryOperatorType.BitAnd -> "&"
            BoundBinaryOperatorType.BitOr -> "|"
            BoundBinaryOperatorType.LogicAnd -> "&&"
            BoundBinaryOperatorType.LogicOr -> "||"
            BoundBinaryOperatorType.LessThan -> "<"
            BoundBinaryOperatorType.MoreThan -> ">"
            BoundBinaryOperatorType.IsEqual -> "=="
            BoundBinaryOperatorType.IsEqualOrMore -> ">="
            BoundBinaryOperatorType.IsEqualOrLess -> "<="
            BoundBinaryOperatorType.IsNotEqual -> "!="
            BoundBinaryOperatorType.IsIdentityEqual -> "==="
            BoundBinaryOperatorType.IsNotIdentityEqual -> "!=="
        }

        fun getString(type: SyntaxType): String = when (type) {
            SyntaxType.Plus -> "+"
            SyntaxType.Minus -> "-"
            SyntaxType.Star -> "*"
            SyntaxType.Div -> "/"
            SyntaxType.Rem -> "%"
            SyntaxType.BitAnd -> "&"
            SyntaxType.BitOr -> "|"
            SyntaxType.LogicAnd -> "&&"
            SyntaxType.LogicOr -> "||"
            SyntaxType.LessThan -> "<"
            SyntaxType.MoreThan -> ">"
            SyntaxType.IsEqual -> "=="
            SyntaxType.IsEqualOrMore -> ">="
            SyntaxType.IsEqualOrLess -> "<="
            SyntaxType.IsNotEqual -> "!="
            SyntaxType.IsIdentityEqual -> "==="
            SyntaxType.IsNotIdentityEqual -> "!=="
            else -> type.toString()
        }
    }
}