package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SeparatedNodeList
import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class LambdaNode(
    syntaxTree: SyntaxTree,
    val params: SeparatedNodeList<ParameterNode>?,
    val typeClause: TypeClauseNode?,
    val arrow: Token?,
    val body: Node?
) : TopLevelNode(syntaxTree) {

    override val kind = SyntaxType.LambdaExpression
    override val children: Collection<Node> get() = arrayListOf<Node>().apply {
        if (params != null) {
            for (param in params) {
                add(param)
            }
        }
        arrow?.let { add(it) }
        typeClause?.let { add(it) }
        body?.let { add(it) }
    }
}