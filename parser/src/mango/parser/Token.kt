package mango.parser

import mango.parser.nodes.Node
import shared.text.TextSpan

open class Token(
        textFile: TextFile,
        override val kind: SyntaxType,
        val position: Int,
        val value: Any? = null,
        val string: String? = null
) : Node(textFile) {
    override val span get() = TextSpan(position, string?.length ?: 0)
    override val children: Collection<Node> get() = emptyList()
}