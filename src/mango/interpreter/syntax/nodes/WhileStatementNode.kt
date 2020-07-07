package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class WhileStatementNode(
        syntaxTree: SyntaxTree,
        val keyword: Token,
        val condition: ExpressionNode,
        val body: BlockStatementNode
) : StatementNode(syntaxTree) {
    override val kind
        get() = SyntaxType.WhileStatement
    override val children: Collection<Node>
        get() = listOf(keyword, condition, body)
}
