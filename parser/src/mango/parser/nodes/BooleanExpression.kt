package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class BoolConstantNode(
    textFile: TextFile,
    val literalToken: Token,
) : Node(textFile) {
    val value: Boolean = literalToken.kind == SyntaxType.True
    override val kind = SyntaxType.BoolConst
    override val children
        get() = listOf(literalToken)
}