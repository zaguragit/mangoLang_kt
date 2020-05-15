package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class AssignmentExpressionNode(
    val identifierToken: Token,
    val equalsToken: Token,
    val expression: ExpressionNode
) : ExpressionNode() {
    override val kind = SyntaxType.AssignmentExpression
    override val children
        get() = listOf(identifierToken, equalsToken, expression)
}