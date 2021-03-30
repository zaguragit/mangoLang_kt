package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class IfNode(
        textFile: TextFile,
        val condition: Node,
        val questionMark: Token,
        val thenExpression: Node,
        val elseClause: ElseClauseNode?
) : Node(textFile) {

    override val kind = SyntaxType.IfExpression
    override val children get() = arrayListOf(condition, questionMark, thenExpression).apply {
        if (elseClause != null) {
            add(elseClause)
        }
    }
}