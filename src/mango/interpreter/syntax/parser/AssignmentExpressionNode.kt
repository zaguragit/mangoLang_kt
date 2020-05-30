package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class AssignmentExpressionNode(
    syntaxTree: SyntaxTree,
    val identifierToken: Token,
    val equalsToken: Token,
    val expression: ExpressionNode
) : ExpressionNode(syntaxTree) {
    override val kind = SyntaxType.AssignmentExpression
    override val children
        get() = listOf(identifierToken, equalsToken, expression)
}