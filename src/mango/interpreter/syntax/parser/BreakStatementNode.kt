package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class BreakStatementNode(
    val keyword: Token
) : StatementNode() {

    override val kind = SyntaxType.BreakStatement
    override val children: Collection<Node> get() = listOf(keyword)
}
