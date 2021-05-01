package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class TextConstantNode(
    textFile: TextFile,
    val literalToken: Token,
    val value: String? = literalToken.value as String?
) : Node(textFile) {
    override val kind = SyntaxType.TextConst
    override val children
        get() = listOf(literalToken)
}