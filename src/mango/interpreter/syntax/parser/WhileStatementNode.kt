package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class WhileStatementNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val condition: ExpressionNode,
    val body: BlockStatementNode
) : StatementNode(syntaxTree) {
    override val kind
        get() = SyntaxType.WhileStatement
    override val children: Collection<Node>
        get() = listOf(keyword, condition, body)
}
