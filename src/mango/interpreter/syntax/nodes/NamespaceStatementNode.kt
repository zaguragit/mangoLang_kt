package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.Token

class NamespaceStatementNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val identifier: Token,
    val openBrace: Token,
    val members: Collection<TopLevelNode>,
    val closedBrace: Token
) : TopLevelNode(syntaxTree) {

    override val kind = SyntaxType.NamespaceStatement
    override val children get() = arrayListOf<Node>(keyword, identifier, openBrace).apply { addAll(members); add(closedBrace) }
}