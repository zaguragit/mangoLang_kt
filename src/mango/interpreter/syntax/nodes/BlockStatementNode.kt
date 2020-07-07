package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

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