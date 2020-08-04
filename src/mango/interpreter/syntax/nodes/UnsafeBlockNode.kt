package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class UnsafeBlockNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val block: BlockNode
) : Node(syntaxTree) {

    override val kind = SyntaxType.UnsafeBlock

    override val children: Collection<Node> get() = listOf(keyword, block)
}