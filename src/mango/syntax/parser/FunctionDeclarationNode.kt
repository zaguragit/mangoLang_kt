package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class FunctionDeclarationNode(
    val keyword: Token,
    val identifier: Token,
    val typeClause: TypeClauseNode?,
    val params: SeparatedNodeList<ParameterNode>?,
    val lambdaArrow: Token?,
    val body: StatementNode
) : MemberNode() {

    override val kind = SyntaxType.FunctionDeclaration
    override val children: Collection<Node> get() = arrayListOf<Node>(keyword, identifier).apply {
        if (params != null) {
            for (param in params) {
                add(param)
            }
        }
        lambdaArrow?.let { add(it) }
        typeClause?.let { add(it) }
        add(body)
    }
}