package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class VariableDeclarationNode(
    val keyword: Token,
    val identifier: Token,
    val typeClauseNode: TypeClauseNode?,
    val equals: Token,
    val initializer: ExpressionNode
) : StatementNode() {

    override val kind
        get() = SyntaxType.VariableDeclaration
    override val children: Collection<Node>
        get() = listOf(keyword, identifier, equals, initializer)
}