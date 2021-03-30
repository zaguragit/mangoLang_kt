package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class AnnotationNode(
        textFile: TextFile,
        val left: Token,
        val identifier: Token,
        val colon: Token?,
        val value: Node?,
        val right: Token
) : Node(textFile) {

    override val kind = SyntaxType.Annotation
    override val children: Collection<Node> get() = arrayListOf<Node>(left, identifier).apply {
        colon?.let { add(it) }
        value?.let { add(it) }
        add(right)
    }
}