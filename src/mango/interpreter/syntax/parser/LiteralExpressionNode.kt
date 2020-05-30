package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class LiteralExpressionNode(
    syntaxTree: SyntaxTree,
    val literalToken: Token,
    val value: Any? = literalToken.value
) : ExpressionNode(syntaxTree) {
    override val kind = SyntaxType.LiteralExpression
    override val children
        get() = listOf(literalToken)
}