package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class StructInitializationNode(
        textFile: TextFile,
        val type: TypeClauseNode,
        val openBrace: Token,
        val params: ArrayList<AssignmentNode>,
        val closedBrace: Token
) : Node(textFile) {

    override val kind = SyntaxType.StructInitialization
    override val children: Collection<Node>
        get() = arrayListOf(type, openBrace).apply {
            addAll(params)
            add(closedBrace)
        }

}
