package mango.interpreter.binding.nodes

import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.syntax.SyntaxType
import kotlin.math.max

data class BoundBiOperator(
    val syntaxType: SyntaxType,
    val type: Type,
    val leftType: TypeSymbol,
    val rightType: TypeSymbol = leftType,
    val resultType: TypeSymbol = leftType
) {

    companion object {

        private val operators = arrayOf(

            BoundBiOperator(SyntaxType.Plus, Type.Add, TypeSymbol.Float),
            BoundBiOperator(SyntaxType.Minus, Type.Sub, TypeSymbol.Float),
            BoundBiOperator(SyntaxType.Star, Type.Mul, TypeSymbol.Float),
            BoundBiOperator(SyntaxType.Div, Type.Div, TypeSymbol.Float),

            BoundBiOperator(SyntaxType.LessThan, Type.LessThan, TypeSymbol.Float, resultType = TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.MoreThan, Type.MoreThan, TypeSymbol.Float, resultType = TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.IsEqualOrLess, Type.IsEqualOrLess, TypeSymbol.Float, resultType = TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.IsEqualOrMore, Type.IsEqualOrMore, TypeSymbol.Float, resultType = TypeSymbol.Bool),

            BoundBiOperator(SyntaxType.Plus, Type.Add, TypeSymbol.Double),
            BoundBiOperator(SyntaxType.Minus, Type.Sub, TypeSymbol.Double),
            BoundBiOperator(SyntaxType.Star, Type.Mul, TypeSymbol.Double),
            BoundBiOperator(SyntaxType.Div, Type.Div, TypeSymbol.Double),

            BoundBiOperator(SyntaxType.LessThan, Type.LessThan, TypeSymbol.Double, resultType = TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.MoreThan, Type.MoreThan, TypeSymbol.Double, resultType = TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.IsEqualOrLess, Type.IsEqualOrLess, TypeSymbol.Double, resultType = TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.IsEqualOrMore, Type.IsEqualOrMore, TypeSymbol.Double, resultType = TypeSymbol.Bool),

            BoundBiOperator(SyntaxType.IsEqual, Type.IsEqual, TypeSymbol.Primitive, resultType = TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.IsNotEqual, Type.IsNotEqual, TypeSymbol.Primitive, resultType = TypeSymbol.Bool),

            BoundBiOperator(SyntaxType.IsIdentityEqual, Type.IsIdentityEqual, TypeSymbol.Any, resultType = TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.IsNotIdentityEqual, Type.IsNotIdentityEqual, TypeSymbol.Any, resultType = TypeSymbol.Bool),

            BoundBiOperator(SyntaxType.BitAnd, Type.BitAnd, TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.BitOr, Type.BitOr, TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.LogicAnd, Type.LogicAnd, TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.LogicOr, Type.LogicOr, TypeSymbol.Bool)
        )

        private val intOperators = arrayOf(

            BoundBiOperator(SyntaxType.Plus, Type.Add, TypeSymbol.AnyI),
            BoundBiOperator(SyntaxType.Minus, Type.Sub, TypeSymbol.AnyI),
            BoundBiOperator(SyntaxType.Star, Type.Mul, TypeSymbol.AnyI),
            BoundBiOperator(SyntaxType.Div, Type.Div, TypeSymbol.AnyI),

            BoundBiOperator(SyntaxType.LessThan, Type.LessThan, TypeSymbol.AnyI, resultType = TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.MoreThan, Type.MoreThan, TypeSymbol.AnyI, resultType = TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.IsEqualOrLess, Type.IsEqualOrLess, TypeSymbol.AnyI, resultType = TypeSymbol.Bool),
            BoundBiOperator(SyntaxType.IsEqualOrMore, Type.IsEqualOrMore, TypeSymbol.AnyI, resultType = TypeSymbol.Bool),

            BoundBiOperator(SyntaxType.BitAnd, Type.BitAnd, TypeSymbol.AnyI),
            BoundBiOperator(SyntaxType.BitOr, Type.BitOr, TypeSymbol.AnyI)
        )

        fun bind(syntaxType: SyntaxType, leftType: TypeSymbol, rightType: TypeSymbol): BoundBiOperator? {
            if (syntaxType == SyntaxType.IsEqual ||
                syntaxType == SyntaxType.IsNotEqual ||
                syntaxType == SyntaxType.IsIdentityEqual ||
                syntaxType == SyntaxType.IsNotIdentityEqual) {
                if (!leftType.isOfType(rightType) && !rightType.isOfType(leftType)) {
                    return null
                }
            }
            if (leftType.isOfType(TypeSymbol.AnyI) && rightType.isOfType(TypeSymbol.AnyI)) {
                for (op in intOperators) {
                    if (op.syntaxType == syntaxType) {
                        when (max(leftType.size, rightType.size)) {
                            8 -> return op.copy(resultType = TypeSymbol.I8)
                            16 -> return op.copy(resultType = TypeSymbol.I16)
                            32 -> return op.copy(resultType = TypeSymbol.I32)
                            64 -> return op.copy(resultType = TypeSymbol.I64)
                        }
                        return op
                    }
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

        fun getString(type: Type): String = when (type) {
            Type.Add -> "+"
            Type.Sub -> "-"
            Type.Mul -> "*"
            Type.Div -> "/"
            Type.Rem -> "%"
            Type.BitAnd -> "&"
            Type.BitOr -> "|"
            Type.LogicAnd -> "&&"
            Type.LogicOr -> "||"
            Type.LessThan -> "<"
            Type.MoreThan -> ">"
            Type.IsEqual -> "=="
            Type.IsEqualOrMore -> ">="
            Type.IsEqualOrLess -> "<="
            Type.IsNotEqual -> "!="
            Type.IsIdentityEqual -> "==="
            Type.IsNotIdentityEqual -> "!=="
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

    enum class Type {
        Add,
        Sub,
        Mul,
        Div,
        Rem,
        BitAnd,
        BitOr,
        LogicAnd,
        LogicOr,

        LessThan,
        MoreThan,
        IsEqual,
        IsEqualOrMore,
        IsEqualOrLess,
        IsNotEqual,
        IsIdentityEqual,
        IsNotIdentityEqual
    }
}