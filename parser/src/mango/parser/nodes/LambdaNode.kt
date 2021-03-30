package mango.parser.nodes

import mango.parser.SeparatedNodeList
import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class LambdaNode(
        textFile: TextFile,
        val open: Token?,
        val params: SeparatedNodeList<ParameterNode>,
        val closed: Token?,
        val returnType: TypeClauseNode?,
        val arrow: Token?,
        val body: Node?,
        val annotations: Collection<AnnotationNode>,
        var declarationNode: ValVarDeclarationNode?
) : TopLevelNode(textFile) {

    override val kind = SyntaxType.LambdaExpression
    override val children: Collection<Node> get() = arrayListOf<Node>().apply {
        open?.let { add(it) }
        for (param in params) {
            add(param)
        }
        closed?.let { add(it) }
        arrow?.let { add(it) }
        returnType?.let { add(it) }
        body?.let { add(it) }
    }
}