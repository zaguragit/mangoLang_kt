package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SeparatedNodeList
import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class FunctionDeclarationNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val identifier: Token,
    val typeClause: TypeClauseNode?,
    val params: SeparatedNodeList<ParameterNode>?,
    val lambdaArrow: Token?,
    val body: StatementNode?,
    val annotations: Collection<AnnotationNode>
) : TopLevelNode(syntaxTree) {

    override val kind = SyntaxType.FunctionDeclaration
    override val children: Collection<Node> get() = arrayListOf<Node>(keyword, identifier).apply {
        if (params != null) {
            for (param in params) {
                add(param)
            }
        }
        lambdaArrow?.let { add(it) }
        typeClause?.let { add(it) }
        body?.let { add(it) }
    }
}