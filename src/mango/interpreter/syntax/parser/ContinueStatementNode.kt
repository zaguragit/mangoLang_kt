package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class ContinueStatementNode(
    val keyword: Token
) : StatementNode() {

    override val kind = SyntaxType.ContinueStatement
    override val children: Collection<Node> get() = listOf(keyword)
}
