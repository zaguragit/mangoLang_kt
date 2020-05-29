package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

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