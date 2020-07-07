package mango.interpreter.binding.nodes

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