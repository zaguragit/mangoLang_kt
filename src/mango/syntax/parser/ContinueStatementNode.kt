package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class ContinueStatementNode(
    val keyword: Token
) : StatementNode() {

    override val kind = SyntaxType.ContinueStatement
    override val children: Collection<Node> get() = listOf(keyword)
}
