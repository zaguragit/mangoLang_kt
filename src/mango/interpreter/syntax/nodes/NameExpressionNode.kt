package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class NameExpressionNode(
        syntaxTree: SyntaxTree,
        val identifierToken: Token
) : ExpressionNode(syntaxTree) {
    override val kind = SyntaxType.NameExpression
    override val children
        get() = listOf(identifierToken)

}