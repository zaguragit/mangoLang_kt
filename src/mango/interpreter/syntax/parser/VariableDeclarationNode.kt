package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class VariableDeclarationNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val identifier: Token,
    val typeClauseNode: TypeClauseNode?,
    val equals: Token,
    val initializer: ExpressionNode
) : StatementNode(syntaxTree) {

    override val kind
        get() = SyntaxType.VariableDeclaration
    override val children: Collection<Node>
        get() = listOf(keyword, identifier, equals, initializer)
}