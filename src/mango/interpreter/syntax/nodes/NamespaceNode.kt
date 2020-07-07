package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.SyntaxTree

class NamespaceNode(
    syntaxTree: SyntaxTree,
    val members: Collection<TopLevelNode>
) : Node(syntaxTree) {

    override val kind = SyntaxType.Namespace
    override val children = members
}