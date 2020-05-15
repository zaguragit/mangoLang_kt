package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class WhileStatementNode(
    val keyword: Token,
    val condition: ExpressionNode,
    val body: BlockStatementNode
) : StatementNode() {
    override val kind
        get() = SyntaxType.WhileStatement
    override val children: Collection<Node>
        get() = listOf(keyword, condition, body)
}
