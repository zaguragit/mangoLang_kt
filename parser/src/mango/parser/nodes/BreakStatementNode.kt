package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class BreakStatementNode(
        textFile: TextFile,
        val keyword: Token
) : Node(textFile) {

    override val kind = SyntaxType.BreakStatement
    override val children: Collection<Node> get() = listOf(keyword)
}
