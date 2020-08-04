package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class ForStatementNode(
        syntaxTree: SyntaxTree,
        val keyword: Token,
        val identifier: Token,
        val inToken: Token,
        val lowerBound: Node,
        val rangeToken: Token,
        val upperBound: Node,
        val body: BlockNode
) : Node(syntaxTree) {

    override val kind = SyntaxType.ForStatement
    override val children: Collection<Node>
        get() = listOf(keyword, identifier, inToken, lowerBound, rangeToken, upperBound, body)
}