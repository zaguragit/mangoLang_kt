package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class VariableDeclarationNode(
        syntaxTree: SyntaxTree,
        val keyword: Token,
        val identifier: Token,
        val typeClauseNode: TypeClauseNode?,
        val equals: Token,
        val initializer: ExpressionNode
) : TopLevelNode(syntaxTree) {

    override val kind = SyntaxType.VariableDeclaration
    override val children: Collection<Node> get() = listOf(keyword, identifier, equals, initializer)
}