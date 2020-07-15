package mango.interpreter.syntax

import mango.interpreter.syntax.nodes.Node
import mango.interpreter.text.TextSpan

open class Token(
    syntaxTree: SyntaxTree,
    override val kind: SyntaxType,
    val position: Int,
    val value: Any? = null,
    val string: String? = null
) : Node(syntaxTree) {
    override val span get() = TextSpan(position, string?.length ?: 0)
    override val children: Collection<Node> get() = emptyList()
}