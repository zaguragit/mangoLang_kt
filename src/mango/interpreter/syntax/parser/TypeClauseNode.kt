package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class TypeClauseNode(
    val identifier: Token
) : Node() {

    override val kind = SyntaxType.TypeClause
    override val children: Collection<Node> get() = listOf(identifier)
}