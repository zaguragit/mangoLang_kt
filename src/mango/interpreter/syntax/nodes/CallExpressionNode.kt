package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SeparatedNodeList
import mango.interpreter.syntax.SyntaxTree

class CallExpressionNode(
    syntaxTree: SyntaxTree,
    identifier: Token,
    val leftBracket: Token,
    val arguments: SeparatedNodeList<ExpressionNode>,
    val rightBracket: Token
) : NameExpressionNode(syntaxTree, identifier) {

    override val kind = SyntaxType.CallExpression
    override val children: Collection<Node> get() = arrayListOf<Node>(
            identifier, leftBracket
        ).apply {
            addAll(arguments.nodesAndSeparators)
            add(rightBracket)
        }
}