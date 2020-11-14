package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class CastExpressionNode(
    syntaxTree: SyntaxTree,
    val expression: Node,
    val keyword: Token,
    val type: TypeClauseNode
) : Node(syntaxTree) {

    override val kind = SyntaxType.CastExpression
    override val children get() = listOf(expression, keyword, type)
}