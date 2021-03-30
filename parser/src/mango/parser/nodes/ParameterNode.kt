package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class ParameterNode(
        textFile: TextFile,
        val identifier: Token,
        val typeClause: TypeClauseNode
) : Node(textFile) {

    override val kind = SyntaxType.Parameter
    override val children: Collection<Node> get() = listOf()
}