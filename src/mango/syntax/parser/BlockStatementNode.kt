package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class BlockStatementNode(val openBrace: Token, val statements: Collection<StatementNode>, val closedBrace: Token) : StatementNode() {
    override val kind
        get() = SyntaxType.BlockStatement
    override val children: Collection<Node>
        get() = ArrayList<Node>().apply {
            add(openBrace)
            addAll(statements)
            add(closedBrace)
        }
}