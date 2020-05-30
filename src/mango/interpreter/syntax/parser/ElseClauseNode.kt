package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class ElseClauseNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val statement: StatementNode
) : Node(syntaxTree) {

    override val kind = SyntaxType.ElseClause

    override val children get() = listOf(keyword, statement)
}