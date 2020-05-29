package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class CallExpressionNode(
    val identifier: Token,
    val leftBracket: Token,
    val arguments: SeparatedNodeList<ExpressionNode>,
    val rightBracket: Token
) : ExpressionNode() {

    override val kind = SyntaxType.CallExpression
    override val children: Collection<Node> get() = arrayListOf<Node>(
        identifier,
        leftBracket).apply {
        addAll(arguments.nodesAndSeparators)
        add(rightBracket)
    }
}