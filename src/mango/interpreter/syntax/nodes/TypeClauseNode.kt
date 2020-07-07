package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class TypeClauseNode(
        syntaxTree: SyntaxTree,
        val identifier: Token
) : Node(syntaxTree) {

    override val kind = SyntaxType.TypeClause
    override val children: Collection<Node> get() = listOf(identifier)
}