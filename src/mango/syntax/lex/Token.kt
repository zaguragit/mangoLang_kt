package mango.syntax.lex

import mango.compilation.TextSpan
import mango.syntax.SyntaxType
import mango.syntax.parser.Node

open class Token(
    override val kind: SyntaxType,
    val position: Int,
    val value: Any? = null,
    val string: String? = null
) : Node() {
    override val span get() = TextSpan(position, string?.length ?: 0)
    override val children: Collection<Node> get() = emptyList()
}