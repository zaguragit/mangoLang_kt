package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType

class NamespaceNode(
    syntaxTree: SyntaxTree,
    val members: Collection<TopLevelNode>
) : TopLevelNode(syntaxTree) {

    override val kind = SyntaxType.Namespace
    override val children = members
}