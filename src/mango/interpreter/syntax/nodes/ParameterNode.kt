package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class ParameterNode(
        syntaxTree: SyntaxTree,
        val identifier: Token,
        val typeClause: TypeClauseNode
) : Node(syntaxTree) {

    override val kind = SyntaxType.Parameter
    override val children: Collection<Node> get() = listOf()
}