package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

open class NameExpressionNode(
        textFile: TextFile,
        val identifier: Token
) : Node(textFile) {

    override val kind = SyntaxType.NameExpression
    override val children: Collection<Node> get() = listOf(identifier)
}