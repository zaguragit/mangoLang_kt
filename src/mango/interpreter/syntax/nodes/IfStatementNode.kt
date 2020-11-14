package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class IfStatementNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val condition: Node,
    val thenStatement: Node,
    val elseClause: ElseClauseNode?
) : Node(syntaxTree) {
    override val kind
        get() = SyntaxType.IfStatement
    override val children
        get() = arrayListOf(keyword, condition, thenStatement).apply {
            if (elseClause != null) {
                add(elseClause)
            }
        }
}