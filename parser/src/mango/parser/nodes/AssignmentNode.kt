package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class AssignmentNode(
    textFile: TextFile,
    val assignee: Node,
    val equalsToken: Token,
    val expression: Node
) : Node(textFile) {
    override val kind = SyntaxType.AssignmentStatement
    override val children
        get() = listOf(assignee, equalsToken, expression)
}