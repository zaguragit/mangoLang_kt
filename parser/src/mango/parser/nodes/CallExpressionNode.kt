package mango.parser.nodes

import mango.parser.SeparatedNodeList
import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class CallExpressionNode(
        textFile: TextFile,
        val function: Node,
        val leftBracket: Token,
        val arguments: SeparatedNodeList<Node>,
        val rightBracket: Token
) : Node(textFile) {

    override val kind = SyntaxType.CallExpression
    override val children get() = arrayListOf(
            function, leftBracket
        ).apply {
            addAll(arguments.nodesAndSeparators)
            add(rightBracket)
        }
}