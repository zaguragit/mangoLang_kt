package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class ParenthesizedExpressionNode(
    syntaxTree: SyntaxTree,
    val open: Token,
    val expression: Node,
    val closed: Token
) : Node(syntaxTree) {
    override val kind = SyntaxType.ParenthesizedExpression
    override val children
        get() = listOf(open, expression, closed)

    override fun toString() = "($expression)"
}