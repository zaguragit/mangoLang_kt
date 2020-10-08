package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SeparatedNodeList
import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class CollectionInitializationNode(
    syntaxTree: SyntaxTree,
    val type: TypeClauseNode,
    val openBrace: Token,
    val expressions: SeparatedNodeList<Node>,
    val closedBrace: Token
) : Node(syntaxTree) {

    override val kind = SyntaxType.CollectionInitialization
    override val children: Collection<Node>
        get() = arrayListOf(type, openBrace).apply {
            addAll(expressions)
            add(closedBrace)
        }

}
