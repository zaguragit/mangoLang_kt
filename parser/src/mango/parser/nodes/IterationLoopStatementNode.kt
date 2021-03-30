package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class IterationLoopStatementNode(
        textFile: TextFile,
        val keyword: Token,
        val identifier: Token,
        val inToken: Token,
        val lowerBound: Node,
        val rangeToken: Token,
        val upperBound: Node,
        val body: Node
) : Node(textFile) {

    override val kind = SyntaxType.IterationLoopStatement
    override val children: Collection<Node>
        get() = listOf(keyword, identifier, inToken, lowerBound, rangeToken, upperBound, body)
}