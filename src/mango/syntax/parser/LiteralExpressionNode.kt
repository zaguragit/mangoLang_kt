package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class LiteralExpressionNode(
    val literalToken: Token,
    val value: Any? = literalToken.value
) : ExpressionNode() {
    override val kind = SyntaxType.LiteralExpression
    override val children
        get() = listOf(literalToken)
}