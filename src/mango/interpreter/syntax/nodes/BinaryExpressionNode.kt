package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class BinaryExpressionNode(
        syntaxTree: SyntaxTree,
        val left: ExpressionNode,
        val operator: Token,
        val right: ExpressionNode
) : ExpressionNode(syntaxTree) {
    override val kind = SyntaxType.BinaryExpression
    override val children
        get() = listOf(left, operator, right)
}