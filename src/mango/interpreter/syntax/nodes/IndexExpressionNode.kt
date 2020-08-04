package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SeparatedNodeList
import mango.interpreter.syntax.SyntaxTree

class IndexExpressionNode(
    syntaxTree: SyntaxTree,
    val expression: Node,
    val leftBracket: Token,
    val arguments: SeparatedNodeList<Node>,
    val rightBracket: Token
) : Node(syntaxTree) {

    override val kind = SyntaxType.IndexExpression
    override val children: Collection<Node> get() = arrayListOf(
            expression, leftBracket
        ).apply {
            addAll(arguments.nodesAndSeparators)
            add(rightBracket)
        }
}