package mango.parser.nodes

import mango.parser.SeparatedNodeList
import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class IndexExpressionNode(
        textFile: TextFile,
        val expression: Node,
        val leftBracket: Token,
        val arguments: SeparatedNodeList<Node>,
        val rightBracket: Token
) : Node(textFile) {

    override val kind = SyntaxType.IndexExpression
    override val children: Collection<Node> get() = arrayListOf(
            expression, leftBracket
        ).apply {
            addAll(arguments.nodesAndSeparators)
            add(rightBracket)
        }
}