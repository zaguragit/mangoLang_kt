package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class FunctionDeclarationNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val identifier: Token,
    val typeClause: TypeClauseNode?,
    val params: SeparatedNodeList<ParameterNode>?,
    val lambdaArrow: Token?,
    val body: StatementNode
) : MemberNode(syntaxTree) {

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