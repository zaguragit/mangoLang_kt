package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class UseStatementNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val path: String,
    val isInclude: Boolean
) : TopLevelNode(syntaxTree) {

    override val kind = SyntaxType.UseStatement
    override val children: Collection<Node> get() = listOf(keyword)
}