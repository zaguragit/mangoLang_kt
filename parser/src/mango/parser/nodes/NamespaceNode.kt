package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile

class NamespaceNode(
        textFile: TextFile,
        val members: Collection<TopLevelNode>
) : TopLevelNode(textFile) {

    override val kind = SyntaxType.Namespace
    override val children = members
}