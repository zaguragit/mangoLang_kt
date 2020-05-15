package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class ElseClauseNode(
        val keyword: Token,
        val statement: BlockStatementNode
) : Node() {
    override val kind
        get() = SyntaxType.ElseClause
    override val children
        get() = listOf(keyword, statement)
}