package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SeparatedNodeList
import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class TypeClauseNode(
    syntaxTree: SyntaxTree,
    val identifier: Token,
    var start: Token?,
    var types: SeparatedNodeList<TypeClauseNode>?,
    var end: Token?
) : Node(syntaxTree) {

    override val kind = SyntaxType.TypeClause
    override val children: Collection<Node> get() = listOf(identifier)
}