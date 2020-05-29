package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class ElseClauseNode(
        val keyword: Token,
        val statement: StatementNode
) : Node() {
    override val kind
        get() = SyntaxType.ElseClause
    override val children
        get() = listOf(keyword, statement)
}