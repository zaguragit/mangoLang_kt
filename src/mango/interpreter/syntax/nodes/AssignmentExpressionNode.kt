package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class AssignmentExpressionNode(
    syntaxTree: SyntaxTree,
    val assignee: Node,
    val equalsToken: Token,
    val expression: Node
) : Node(syntaxTree) {
    override val kind = SyntaxType.AssignmentExpression
    override val children
        get() = listOf(assignee, equalsToken, expression)
}