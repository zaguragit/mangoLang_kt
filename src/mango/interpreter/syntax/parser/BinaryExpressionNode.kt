package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class BinaryExpressionNode(
    val left: ExpressionNode,
    val operator: Token,
    val right: ExpressionNode
) : ExpressionNode() {
    override val kind = SyntaxType.BinaryExpression
    override val children
        get() = listOf(left, operator, right)
}