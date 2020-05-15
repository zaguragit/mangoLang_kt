package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class IfStatementNode(
    val keyword: Token,
    val condition: ExpressionNode,
    val thenStatement: BlockStatementNode,
    val elseClause: ElseClauseNode?
) : StatementNode() {
    override val kind
        get() = SyntaxType.IfStatement
    override val children
        get() = arrayListOf(keyword, condition, thenStatement).apply {
            if (elseClause != null) {
                add(elseClause)
            }
        }
}