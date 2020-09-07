package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class AssignmentNode(
    syntaxTree: SyntaxTree,
    val assignee: Node,
    val equalsToken: Token,
    val expression: Node
) : Node(syntaxTree) {
    override val kind = SyntaxType.AssignmentStatement
    override val children
        get() = listOf(assignee, equalsToken, expression)
}