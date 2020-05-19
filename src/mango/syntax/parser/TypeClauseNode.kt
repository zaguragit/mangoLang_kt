package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class TypeClauseNode(
    val identifier: Token
) : Node() {

    override val kind = SyntaxType.TypeClause
    override val children: Collection<Node> get() = listOf(identifier)
}