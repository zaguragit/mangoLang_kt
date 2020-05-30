package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class UnaryExpressionNode(
    syntaxTree: SyntaxTree,
    val operator: Token,
    val operand: ExpressionNode
) : ExpressionNode(syntaxTree) {
    override val kind: SyntaxType = SyntaxType.UnaryExpression
    override val children: Collection<Node>
        get() = listOf(operator, operand)
}