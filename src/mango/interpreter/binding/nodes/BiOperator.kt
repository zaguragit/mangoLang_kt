package mango.interpreter.binding.nodes

import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.syntax.SyntaxType
import kotlin.math.max

data class BiOperator(
    val syntaxType: SyntaxType,
    val type: Type,
    val leftType: TypeSymbol,
    val rightType: TypeSymbol = leftType,
    val resultType: TypeSymbol = leftType
) {

    companion object {

        private val operators = arrayOf(

            BiOperator(SyntaxType.Plus, Type.Add, TypeSymbol.Float),
            BiOperator(SyntaxType.Minus, Type.Sub, TypeSymbol.Float),
            BiOperator(SyntaxType.Star, Type.Mul, TypeSymbol.Float),
            BiOperator(SyntaxType.Div, Type.Div, TypeSymbol.Float),

            BiOperator(SyntaxType.LessThan, Type.LessThan, TypeSymbol.Float, resultType = TypeSymbol.Bool),
            BiOperator(SyntaxType.MoreThan, Type.MoreThan, TypeSymbol.Float, resultType = TypeSymbol.Bool),
            BiOperator(SyntaxType.IsEqualOrLess, Type.IsEqualOrLess, TypeSymbol.Float, resultType = TypeSymbol.Bool),
            BiOperator(SyntaxType.IsEqualOrMore, Type.IsEqualOrMore, TypeSymbol.Float, resultType = TypeSymbol.Bool),

            BiOperator(SyntaxType.Plus, Type.Add, TypeSymbol.Double),
            BiOperator(SyntaxType.Minus, Type.Sub, TypeSymbol.Double),
            BiOperator(SyntaxType.Star, Type.Mul, TypeSymbol.Double),
            BiOperator(SyntaxType.Div, Type.Div, TypeSymbol.Double),

            BiOperator(SyntaxType.LessThan, Type.LessThan, TypeSymbol.Double, resultType = TypeSymbol.Bool),
            BiOperator(SyntaxType.MoreThan, Type.MoreThan, TypeSymbol.Double, resultType = TypeSymbol.Bool),
            BiOperator(SyntaxType.IsEqualOrLess, Type.IsEqualOrLess, TypeSymbol.Double, resultType = TypeSymbol.Bool),
            BiOperator(SyntaxType.IsEqualOrMore, Type.IsEqualOrMore, TypeSymbol.Double, resultType = TypeSymbol.Bool),

            BiOperator(SyntaxType.IsEqual, Type.IsEqual, TypeSymbol.Primitive, resultType = TypeSymbol.Bool),
            BiOperator(SyntaxType.IsNotEqual, Type.IsNotEqual, TypeSymbol.Primitive, resultType = TypeSymbol.Bool),

            BiOperator(SyntaxType.IsIdentityEqual, Type.IsIdentityEqual, TypeSymbol.Any, resultType = TypeSymbol.Bool),
            BiOperator(SyntaxType.IsNotIdentityEqual, Type.IsNotIdentityEqual, TypeSymbol.Any, resultType = TypeSymbol.Bool),

            BiOperator(SyntaxType.BitAnd, Type.BitAnd, TypeSymbol.Bool),
            BiOperator(SyntaxType.BitOr, Type.BitOr, TypeSymbol.Bool),
            BiOperator(SyntaxType.LogicAnd, Type.LogicAnd, TypeSymbol.Bool),
            BiOperator(SyntaxType.LogicOr, Type.LogicOr, TypeSymbol.Bool)
        )

        private val intOperators = arrayOf(

            BiOperator(SyntaxType.Plus, Type.Add, TypeSymbol.Integer),
            BiOperator(SyntaxType.Minus, Type.Sub, TypeSymbol.Integer),
            BiOperator(SyntaxType.Star, Type.Mul, TypeSymbol.Integer),
            BiOperator(SyntaxType.Div, Type.Div, TypeSymbol.Integer),

            BiOperator(SyntaxType.LessThan, Type.LessThan, TypeSymbol.Integer, resultType = TypeSymbol.Bool),
            BiOperator(SyntaxType.MoreThan, Type.MoreThan, TypeSymbol.Integer, resultType = TypeSymbol.Bool),
            BiOperator(SyntaxType.IsEqualOrLess, Type.IsEqualOrLess, TypeSymbol.Integer, resultType = TypeSymbol.Bool),
            BiOperator(SyntaxType.IsEqualOrMore, Type.IsEqualOrMore, TypeSymbol.Integer, resultType = TypeSymbol.Bool),

            BiOperator(SyntaxType.BitAnd, Type.BitAnd, TypeSymbol.Integer),
            BiOperator(SyntaxType.BitOr, Type.BitOr, TypeSymbol.Integer)
        )

        fun bind(syntaxType: SyntaxType, leftType: TypeSymbol, rightType: TypeSymbol): BiOperator? {
            if (syntaxType == SyntaxType.IsEqual ||
                syntaxType == SyntaxType.IsNotEqual ||
                syntaxType == SyntaxType.IsIdentityEqual ||
                syntaxType == SyntaxType.IsNotIdentityEqual) {
                if (!leftType.isOfType(rightType) && !rightType.isOfType(leftType)) {
                    return null
                }
            }
            if (leftType.isOfType(TypeSymbol.Integer) && rightType.isOfType(TypeSymbol.Integer)) {
                for (op in intOperators) {
                    if (op.syntaxType == syntaxType) {
                        if (op.resultType == TypeSymbol.Integer) {
                            when (max(leftType.size, rightType.size)) {
                                8 -> return op.copy(resultType = TypeSymbol.I8)
                                16 -> return op.copy(resultType = TypeSymbol.I16)
                                32 -> return op.copy(resultType = TypeSymbol.I32)
                                64 -> return op.copy(resultType = TypeSymbol.I64)
                            }
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