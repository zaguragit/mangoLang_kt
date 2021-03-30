package mango.parser

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
    NameExpression,
    CallExpression,
    IndexExpression,
    StructInitialization,
    CollectionInitialization,
    CastExpression,
    IfExpression,
    LambdaExpression,

    // Literals
    I8,
    I16,
    I32,
    I64,
    Float,
    Double,
    String,
    Char,

    // Statements
    ExpressionStatement,
    LoopStatement,
    IterationLoopStatement,
    BreakStatement,
    ContinueStatement,
    ReturnStatement,
    ValVarDeclaration,
    TypeDeclaration,
    UseStatement,
    NamespaceStatement,
    AssignmentStatement,

    Block,

    // Brackets
    OpenParentheses,
    ClosedParentheses,
    OpenBrace,
    ClosedBrace,
    OpenBracket,
    ClosedBracket,

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
    Bang,
    LessThan,
    MoreThan,
    IsEqual,
    IsEqualOrMore,
    IsEqualOrLess,
    IsNotEqual,
    IsIdentityEqual,
    IsNotIdentityEqual,
    As,
    Range,
    DoubleBang,
    PlusPlus,
    MinusMinus,

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
    NamespaceToken,
    Type,

    // Loop keywords
    Loop,
    Break,
    Continue,
    Return,

    Colon,
    QuestionMark,

    Identifier,
    TypeClause,

    Use,

    Unsafe,

    SingleLineComment,
    MultilineComment;

    fun getBinaryOperatorPrecedence() = when (this) {
        Dot -> 8
        Star, Div -> 7
        Plus, Minus, Rem -> 6
        BitAnd -> 5
        BitOr -> 4
        LessThan, MoreThan, IsEqual, IsEqualOrMore, IsEqualOrLess, IsNotEqual, IsIdentityEqual, IsNotIdentityEqual -> 3
        LogicAnd -> 2
        LogicOr -> 1
        QuestionMark -> 1
        else -> 0
    }

    fun getUnaryOperatorPrecedence() = when (this) {
        Plus, Minus, Bang, BitAnd -> 9
        else -> 0
    }
}