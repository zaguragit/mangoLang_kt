package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class ForStatementNode(
    val keyword: Token,
    val identifier: Token,
    val inToken: Token,
    val lowerBound: ExpressionNode,
    val rangeToken: Token,
    val upperBound: ExpressionNode,
    val body: BlockStatementNode
) : StatementNode() {

    override val kind
        get() = SyntaxType.ForStatement
    override val children: Collection<Node>
        get() = listOf(keyword, identifier, inToken, lowerBound, rangeToken, upperBound, body)
}