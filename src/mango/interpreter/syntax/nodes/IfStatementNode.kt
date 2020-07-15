package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class IfStatementNode(
        syntaxTree: SyntaxTree,
        val keyword: Token,
        val condition: ExpressionNode,
        val thenStatement: BlockStatementNode,
        val elseClause: ElseClauseNode?
) : StatementNode(syntaxTree) {
    override val kind
        get() = SyntaxType.IfStatement
    override val children
        get() = arrayListOf(keyword, condition, thenStatement).apply {
            if (elseClause != null) {
                add(elseClause)
            }
        }
}