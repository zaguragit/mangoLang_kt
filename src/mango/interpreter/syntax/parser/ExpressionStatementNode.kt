package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType

class ExpressionStatementNode(val expression: ExpressionNode) : StatementNode() {
    override val kind
        get() = SyntaxType.ExpressionStatement
    override val children: Collection<Node>
        get() = listOf(expression)
}