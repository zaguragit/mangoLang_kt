package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class ContinueStatementNode(
    syntaxTree: SyntaxTree,
    val keyword: Token
) : StatementNode(syntaxTree) {

    override val kind = SyntaxType.ContinueStatement
    override val children: Collection<Node> get() = listOf(keyword)
}
