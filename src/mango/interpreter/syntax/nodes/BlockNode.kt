package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class BlockNode(
    syntaxTree: SyntaxTree,
    val keyword: Token?,
    val openBrace: Token,
    val statements: Collection<Node>,
    val closedBrace: Token,
    val isUnsafe: Boolean
) : Node(syntaxTree) {

    override val kind = SyntaxType.Block

    override val children: Collection<Node>
        get() = ArrayList<Node>().apply {
            add(openBrace)
            addAll(statements)
            add(closedBrace)
        }
}