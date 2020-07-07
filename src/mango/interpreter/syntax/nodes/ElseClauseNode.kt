package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class ElseClauseNode(
        syntaxTree: SyntaxTree,
        val keyword: Token,
        val statement: StatementNode
) : Node(syntaxTree) {

    override val kind = SyntaxType.ElseClause

    override val children get() = listOf(keyword, statement)
}