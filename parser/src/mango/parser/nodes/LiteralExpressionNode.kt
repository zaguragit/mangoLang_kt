package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class LiteralExpressionNode(
        textFile: TextFile,
        val literalToken: Token,
        val value: Any? = literalToken.value
) : Node(textFile) {
    override val kind = SyntaxType.LiteralExpression
    override val children
        get() = listOf(literalToken)
}