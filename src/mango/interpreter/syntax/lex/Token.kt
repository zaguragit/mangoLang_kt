package mango.interpreter.syntax.lex

import mango.interpreter.text.TextSpan
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.parser.Node
import mango.interpreter.syntax.parser.SyntaxTree
import mango.interpreter.text.TextLocation

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