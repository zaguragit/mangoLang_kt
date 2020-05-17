package mango.binding

class BoundBinaryExpression(
    val left: BoundExpression,
    val operator: BoundBinaryOperator,
    val right: BoundExpression
) : BoundExpression() {
    override val type get() = operator.resultType
    override val boundType = BoundNodeType.BinaryExpression
    override val children get() = listOf(left, right)
}

enum class BoundBinaryOperatorType {
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