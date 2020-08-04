package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class BinaryExpressionNode(
        syntaxTree: SyntaxTree,
        val left: Node,
        val operator: Token,
        val right: Node
) : Node(syntaxTree) {
    override val kind = SyntaxType.BinaryExpression
    override val children
        get() = listOf(left, operator, right)
}