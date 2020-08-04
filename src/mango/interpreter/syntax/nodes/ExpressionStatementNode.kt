package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.SyntaxTree

class ExpressionStatementNode(
    syntaxTree: SyntaxTree,
    val expression: Node
) : Node(syntaxTree) {

    override val kind = SyntaxType.ExpressionStatement
    override val children: Collection<Node> get() = listOf(expression)
}