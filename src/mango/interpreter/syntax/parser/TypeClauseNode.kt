package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class TypeClauseNode(
    syntaxTree: SyntaxTree,
    val identifier: Token
) : Node(syntaxTree) {

    override val kind = SyntaxType.TypeClause
    override val children: Collection<Node> get() = listOf(identifier)
}