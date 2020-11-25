package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class IfNode(
    syntaxTree: SyntaxTree,
    val condition: Node,
    val questionMark: Token,
    val thenExpression: Node,
    val elseClause: ElseClauseNode?
) : Node(syntaxTree) {
    override val kind
        get() = SyntaxType.IfExpression
    override val children
        get() = arrayListOf(questionMark, condition, thenExpression).apply {
            if (elseClause != null) {
                add(elseClause)
            }
        }
}