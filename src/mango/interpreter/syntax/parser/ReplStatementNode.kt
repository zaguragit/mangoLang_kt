package mango.interpreter.syntax.parser

class ReplStatementNode(
    val statementNode: StatementNode
) : TopLevelNode(statementNode.syntaxTree) {

    override val kind = statementNode.kind
    override val children get() = statementNode.children
}