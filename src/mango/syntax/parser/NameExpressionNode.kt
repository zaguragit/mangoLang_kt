package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class NameExpressionNode(
    val identifierToken: Token
) : ExpressionNode() {
    override val kind = SyntaxType.NameExpression
    override val children
        get() = listOf(identifierToken)

}