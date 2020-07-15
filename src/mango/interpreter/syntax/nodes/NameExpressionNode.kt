package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

open class NameExpressionNode(
    syntaxTree: SyntaxTree,
    val identifier: Token
) : ExpressionNode(syntaxTree) {

    override val kind = SyntaxType.NameExpression
    override val children: Collection<Node> get() = listOf(identifier)
}