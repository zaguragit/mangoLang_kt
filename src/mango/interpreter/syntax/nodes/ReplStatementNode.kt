package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType

class ReplStatementNode(
    val statementNode: Node
) : TopLevelNode(statementNode.syntaxTree) {

    override val kind = SyntaxType.ReplStatement
    override val children get() = statementNode.children
}