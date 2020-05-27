package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class ReturnStatementNode(
    val keyword: Token,
    val expression: ExpressionNode?
) : StatementNode() {

    override val kind = SyntaxType.ReturnStatement
    override val children: Collection<Node> get() = arrayListOf<Node>(keyword).apply { expression?.let { add(it) } }
}