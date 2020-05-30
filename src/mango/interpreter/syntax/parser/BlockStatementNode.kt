package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class BlockStatementNode(
    syntaxTree: SyntaxTree,
    val openBrace: Token,
    val statements: Collection<StatementNode>,
    val closedBrace: Token
) : StatementNode(syntaxTree) {

    override val kind = SyntaxType.BlockStatement

    override val children: Collection<Node>
        get() = ArrayList<Node>().apply {
            add(openBrace)
            addAll(statements)
            add(closedBrace)
        }
}