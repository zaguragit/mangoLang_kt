package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class BinaryExpressionNode(
    val left: ExpressionNode,
    val operator: Token,
    val right: ExpressionNode
) : ExpressionNode() {
    override val kind = SyntaxType.BinaryExpression
    override val children
        get() = listOf(left, operator, right)
}