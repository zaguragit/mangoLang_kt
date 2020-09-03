package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SeparatedNodeList
import mango.interpreter.syntax.SyntaxTree

class CallExpressionNode(
    syntaxTree: SyntaxTree,
    val function: Node,
    val leftBracket: Token,
    val arguments: SeparatedNodeList<Node>,
    val rightBracket: Token
) : Node(syntaxTree) {

    override val kind = SyntaxType.CallExpression
    override val children get() = arrayListOf(
            function, leftBracket
        ).apply {
            addAll(arguments.nodesAndSeparators)
            add(rightBracket)
        }
}