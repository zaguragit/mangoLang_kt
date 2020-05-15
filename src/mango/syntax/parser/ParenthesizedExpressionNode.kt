package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class ParenthesizedExpressionNode(
    val open: Token,
    val expression: ExpressionNode,
    val closed: Token
) : ExpressionNode() {
    override val kind = SyntaxType.ParenthesizedExpression
    override val children
        get() = listOf(open, expression, closed)

    override fun toString() = "($expression)"
}