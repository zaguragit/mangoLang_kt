package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class ReturnStatementNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val expression: ExpressionNode?
) : StatementNode(syntaxTree) {

    override val kind = SyntaxType.ReturnStatement
    override val children: Collection<Node> get() = arrayListOf<Node>(keyword).apply { expression?.let { add(it) } }
}