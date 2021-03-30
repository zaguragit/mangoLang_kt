package mango.parser.nodes

import mango.parser.SeparatedNodeList
import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class TypeClauseNode(
        textFile: TextFile,
        val identifier: SeparatedNodeList<Token>,
        var start: Token?,
        var types: SeparatedNodeList<TypeClauseNode>?,
        var end: Token?
) : Node(textFile) {

    override val kind = SyntaxType.TypeClause
    override val children: Collection<Node> get() = identifier.nodesAndSeparators
}