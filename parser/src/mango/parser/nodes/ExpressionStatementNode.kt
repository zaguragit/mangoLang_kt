package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile

class ExpressionStatementNode(
        textFile: TextFile,
        val expression: Node
) : Node(textFile) {

    override val kind = SyntaxType.ExpressionStatement
    override val children: Collection<Node> get() = listOf(expression)
}