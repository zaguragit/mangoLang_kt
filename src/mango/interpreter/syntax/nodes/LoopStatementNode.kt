package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class LoopStatementNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val body: Node
) : Node(syntaxTree) {
    override val kind
        get() = SyntaxType.LoopStatement
    override val children: Collection<Node>
        get() = listOf(keyword, body)
}
