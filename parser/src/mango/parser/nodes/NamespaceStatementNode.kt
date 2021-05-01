package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class NamespaceStatementNode(
    textFile: TextFile,
    val keyword: Token,
    val identifier: Token,
    val openBrace: Token,
    val members: Collection<TopLevelNode>,
    val closedBrace: Token
) : TopLevelNode(textFile) {

    override val kind = SyntaxType.NamespaceStatement
    override val children get() = arrayListOf<Node>(keyword, identifier, openBrace).apply { addAll(members); add(closedBrace) }
}