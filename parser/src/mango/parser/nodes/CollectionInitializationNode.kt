package mango.parser.nodes

import mango.parser.SeparatedNodeList
import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class CollectionInitializationNode(
        textFile: TextFile,
        val type: TypeClauseNode,
        val openBrace: Token,
        val expressions: SeparatedNodeList<Node>,
        val closedBrace: Token
) : Node(textFile) {

    override val kind = SyntaxType.CollectionInitialization
    override val children: Collection<Node>
        get() = arrayListOf(type, openBrace).apply {
            addAll(expressions)
            add(closedBrace)
        }

}
