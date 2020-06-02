package mango.interpreter.syntax

enum class SyntaxType {

    EOF,
    Bad,
    LineSeparator,
    Dot,
    Comma,

    // Nodes
    CompilationUnit,
    ElseClause,
    Parameter,

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
    As,
    Is,
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
    TypeClause;

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