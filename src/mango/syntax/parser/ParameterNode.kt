package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class ParameterNode(
        val identifier: Token,
        val typeClause: TypeClauseNode
) : Node() {

    override val kind = SyntaxType.Parameter
    override val children: Collection<Node> get() = listOf()
}