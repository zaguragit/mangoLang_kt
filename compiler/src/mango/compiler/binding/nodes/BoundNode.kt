package mango.compiler.binding.nodes

abstract class BoundNode {

    abstract val kind: Kind

    enum class Kind {
        UnaryExpression,
        BinaryExpression,
        LiteralExpression,
        NameExpression,
        CallExpression,
        ErrorExpression,
        CastExpression,
        NamespaceFieldAccess,
        StructFieldAccess,
        BlockExpression,
        ReferenceExpression,
        PointerAccessExpression,
        StructInitialization,
        PointerArrayInitialization,
        IfExpression,
        Lambda,

        ExpressionStatement,
        ValVarDeclaration,
        LoopStatement,
        ForLoopStatement,
        LabelStatement,
        GotoStatement,
        ConditionalGotoStatement,
        ReturnStatement,
        AssignmentStatement,
        PointerAccessAssignment,
        NopStatement
    }
}