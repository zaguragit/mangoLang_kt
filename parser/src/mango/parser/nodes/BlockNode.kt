package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class BlockNode(
        textFile: TextFile,
        val keyword: Token?,
        val openBrace: Token,
        val statements: Collection<Node>,
        val closedBrace: Token,
        val isUnsafe: Boolean
) : Node(textFile) {

    override val kind = SyntaxType.Block

    override val children: Collection<Node>
        get() = ArrayList<Node>().apply {
            keyword?.let { add(it) }
            add(openBrace)
            addAll(statements)
            add(closedBrace)
        }
}