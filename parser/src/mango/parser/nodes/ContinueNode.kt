package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class ContinueNode(
    textFile: TextFile,
    val keyword: Token
) : Node(textFile) {

    override val kind = SyntaxType.ContinueStatement
    override val children: Collection<Node> get() = listOf(keyword)
}
