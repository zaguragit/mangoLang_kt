package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class CastExpressionNode(
        textFile: TextFile,
        val expression: Node,
        val keyword: Token,
        val type: TypeClauseNode
) : Node(textFile) {

    override val kind = SyntaxType.CastExpression
    override val children get() = listOf(expression, keyword, type)
}