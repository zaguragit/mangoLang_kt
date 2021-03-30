package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class UnaryExpressionNode(
        textFile: TextFile,
        val operator: Token,
        val operand: Node
) : Node(textFile) {
    override val kind: SyntaxType = SyntaxType.UnaryExpression
    override val children: Collection<Node>
        get() = listOf(operator, operand)
}