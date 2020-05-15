package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class UnaryExpressionNode(
    val operator: Token,
    val operand: ExpressionNode
) : ExpressionNode() {
    override val kind: SyntaxType = SyntaxType.UnaryExpression
    override val children: Collection<Node>
        get() = listOf(operator, operand)
}