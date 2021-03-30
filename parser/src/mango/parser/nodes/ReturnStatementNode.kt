package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class ReturnStatementNode(
        textFile: TextFile,
        val keyword: Token,
        val expression: Node?
) : Node(textFile) {

    override val kind = SyntaxType.ReturnStatement
    override val children: Collection<Node> get() = arrayListOf<Node>(keyword).apply { expression?.let { add(it) } }
}