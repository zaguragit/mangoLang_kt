package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType

class NamespaceNode(
    syntaxTree: SyntaxTree,
    val members: Collection<TopLevelNode>
) : Node(syntaxTree) {

    override val kind = SyntaxType.Namespace
    override val children = members
}