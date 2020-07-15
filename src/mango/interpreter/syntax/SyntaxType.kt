package mango.interpreter.syntax

enum class SyntaxType {

    EOF,
    Bad,
    LineSeparator,
    Comma,

    // Nodes
    Namespace,
    ElseClause,
    Parameter,
    Annotation,

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
    IfStatement,
    WhileStatement,
    ForStatement,
    BreakStatement,
    ContinueStatement,
    ReturnStatement,
    VariableDeclaration,
    FunctionDeclaration,
    UseStatement,

    // Brackets
    OpenRoundedBracket,
    ClosedRoundedBracket,
    OpenCurlyBracket,
    ClosedCurlyBracket,
    OpenSquareBracket,
    ClosedSquareBracket,

    // Operators
    Dot,
    Plus,
    Minus,
    Star,
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

    LambdaArrow,

    // Literals
    True,
    False,
    Null,

    // Declaration keywords
    Val,
    Var,
    Fn,
    Colon,

    // Conditional keywords
    If,
    Else,
    For,
    While,

    Break,
    Continue,
    Return,

    Identifier,
    TypeClause,

    Use,

    SingleLineComment,
    MultilineComment;

    fun getBinaryOperatorPrecedence() = when (this) {
        Dot -> 8
        BitAnd -> 7
        BitOr -> 6
        LessThan, MoreThan, IsEqual, IsEqualOrMore, IsEqualOrLess, IsNotEqual, IsIdentityEqual, IsNotIdentityEqual -> 5
        Star, Div -> 4
        Plus, Minus -> 3
        LogicAnd -> 2
        LogicOr -> 1
        else -> 0
    }

    fun getUnaryOperatorPrecedence() = when (this) {
        Plus, Minus, Not -> 9
        else -> 0
    }
}