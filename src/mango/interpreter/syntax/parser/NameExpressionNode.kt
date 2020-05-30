package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class NameExpressionNode(
    syntaxTree: SyntaxTree,
    val identifierToken: Token
) : ExpressionNode(syntaxTree) {
    override val kind = SyntaxType.NameExpression
    override val children
        get() = listOf(identifierToken)

}