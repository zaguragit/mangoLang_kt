package mango.syntax

enum class SyntaxType {

    EOF,
    Bad,
    NewLine,
    Dot,
    Comma,

    // Nodes
    FileUnit,
    ElseClause,

    // Expressions
    LiteralExpression,
    BinaryExpression,
    UnaryExpression,
    ParenthesizedExpression,
    NameExpression,
    AssignmentExpression,
    CallExpression,

    // Types
    Int,
    String,

    // Statements
    BlockStatement,
    ExpressionStatement,
    VariableDeclaration,
    IfStatement,
    WhileStatement,
    ForStatement,

    // Brackets
    OpenRoundedBracket,
    ClosedRoundedBracket,
    OpenCurlyBracket,
    ClosedCurlyBracket,
    OpenSquareBracket,
    ClosedSquareBracket,

    // Operators
    Plus,
    Minus,
    Mul,
    Div,
    Rem,
    BitAnd,
    BitOr,
    LogicAnd,
    LogicOr,
    Not,
    LessThan,
    MoreThan,
    IsEqual,
    IsEqualOrMore,
    IsEqualOrLess,
    IsNotEqual,
    IsIdentityEqual,
    IsNotIdentityEqual,
    In,
    Range,

    // Assignment operators
    Equals,
    PlusEquals,
    MinusEquals,
    TimesEquals,
    DivEquals,
    RemEquals,
    AndEquals,
    OrEquals,

    // Literals
    True,
    False,
    Null,

    // Declaration keywords
    Val,
    Var,

    // Conditional keywords
    If,
    Else,
    For,
    While,

    Identifier;

    fun getBinaryOperatorPrecedence() = when (this) {
        Mul, Div -> 7
        Plus, Minus -> 6
        BitAnd -> 5
        BitOr -> 4
        LessThan,
        MoreThan,
        IsEqual,
        IsEqualOrMore,
        IsEqualOrLess,
        IsNotEqual,
        IsIdentityEqual,
        IsNotIdentityEqual -> 3
        LogicAnd -> 2
        LogicOr -> 1
        else -> 0
    }

    fun getUnaryOperatorPrecedence() = when (this) {
        Plus, Minus, Not -> 8
        else -> 0
    }
}