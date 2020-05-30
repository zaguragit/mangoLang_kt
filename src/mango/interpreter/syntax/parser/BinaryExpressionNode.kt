package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

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