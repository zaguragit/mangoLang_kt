package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class BlockNode(
    syntaxTree: SyntaxTree,
    val openBrace: Token,
    val statements: Collection<Node>,
    val closedBrace: Token
) : Node(syntaxTree) {

    override val kind = SyntaxType.Block

    override val children: Collection<Node>
        get() = ArrayList<Node>().apply {
            add(openBrace)
            addAll(statements)
            add(closedBrace)
        }
}