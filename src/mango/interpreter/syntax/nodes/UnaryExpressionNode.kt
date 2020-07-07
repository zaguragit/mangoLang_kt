package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class UnaryExpressionNode(
        syntaxTree: SyntaxTree,
        val operator: Token,
        val operand: ExpressionNode
) : ExpressionNode(syntaxTree) {
    override val kind: SyntaxType = SyntaxType.UnaryExpression
    override val children: Collection<Node>
        get() = listOf(operator, operand)
}