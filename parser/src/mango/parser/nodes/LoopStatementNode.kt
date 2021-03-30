package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class LoopStatementNode(
        textFile: TextFile,
        val keyword: Token,
        val body: Node
) : Node(textFile) {
    override val kind
        get() = SyntaxType.LoopStatement
    override val children: Collection<Node>
        get() = listOf(keyword, body)
}
