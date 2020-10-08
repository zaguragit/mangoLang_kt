package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class StructInitializationNode(
    syntaxTree: SyntaxTree,
    val type: TypeClauseNode,
    val openBrace: Token,
    val params: ArrayList<AssignmentNode>,
    val closedBrace: Token
) : Node(syntaxTree) {

    override val kind = SyntaxType.StructInitialization
    override val children: Collection<Node>
        get() = arrayListOf(type, openBrace).apply {
            addAll(params)
            add(closedBrace)
        }

}
