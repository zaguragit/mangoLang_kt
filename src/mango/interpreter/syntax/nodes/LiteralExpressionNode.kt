package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class LiteralExpressionNode(
    syntaxTree: SyntaxTree,
    val literalToken: Token,
    val value: Any? = literalToken.value
) : Node(syntaxTree) {
    override val kind = SyntaxType.LiteralExpression
    override val children
        get() = listOf(literalToken)
}