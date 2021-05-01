package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class AnnotationNode(
    textFile: TextFile,
    val character: Token,
    val identifier: Token,
    val callSyntax: CallExpressionNode?,
) : Node(textFile) {

    override val kind = SyntaxType.Annotation
    override val children: Collection<Node> get() = listOf(character, callSyntax ?: identifier)

    fun getIdentifierString(): String = identifier.string!!

    fun getParameter(i: Int): Node? = callSyntax?.arguments?.getOrNull(i)
}