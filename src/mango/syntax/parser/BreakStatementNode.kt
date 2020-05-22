package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class BreakStatementNode(
    val keyword: Token
) : StatementNode() {

    override val kind = SyntaxType.BreakStatement
    override val children: Collection<Node> get() = listOf(keyword)
}
