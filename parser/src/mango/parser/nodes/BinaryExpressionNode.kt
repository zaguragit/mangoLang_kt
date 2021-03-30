package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class BinaryExpressionNode(
        textFile: TextFile,
        val left: Node,
        val operator: Token,
        val right: Node
) : Node(textFile) {
    override val kind = SyntaxType.BinaryExpression
    override val children
        get() = listOf(left, operator, right)
}