package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class ParenthesizedExpressionNode(
    syntaxTree: SyntaxTree,
    val open: Token,
    val expression: ExpressionNode,
    val closed: Token
) : ExpressionNode(syntaxTree) {
    override val kind = SyntaxType.ParenthesizedExpression
    override val children
        get() = listOf(open, expression, closed)

    override fun toString() = "($expression)"
}