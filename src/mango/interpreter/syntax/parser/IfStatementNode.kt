package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

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