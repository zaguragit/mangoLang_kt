package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class ElseClauseNode(
        textFile: TextFile,
        val colon: Token,
        val expression: Node
) : Node(textFile) {

    override val kind = SyntaxType.ElseClause

    override val children get() = listOf(colon, expression)
}