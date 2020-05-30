package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class ForStatementNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val identifier: Token,
    val inToken: Token,
    val lowerBound: ExpressionNode,
    val rangeToken: Token,
    val upperBound: ExpressionNode,
    val body: BlockStatementNode
) : StatementNode(syntaxTree) {

    override val kind = SyntaxType.ForStatement
    override val children: Collection<Node>
        get() = listOf(keyword, identifier, inToken, lowerBound, rangeToken, upperBound, body)
}