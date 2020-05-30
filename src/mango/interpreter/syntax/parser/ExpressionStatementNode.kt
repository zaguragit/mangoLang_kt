package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType

class ExpressionStatementNode(
    syntaxTree: SyntaxTree,
    val expression: ExpressionNode
) : StatementNode(syntaxTree) {

    override val kind = SyntaxType.ExpressionStatement
    override val children: Collection<Node> get() = listOf(expression)
}