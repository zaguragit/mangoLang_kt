package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class ParameterNode(
    syntaxTree: SyntaxTree,
    val identifier: Token,
    val typeClause: TypeClauseNode
) : Node(syntaxTree) {

    override val kind = SyntaxType.Parameter
    override val children: Collection<Node> get() = listOf()
}