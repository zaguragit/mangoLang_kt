package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class ReturnStatementNode(
        syntaxTree: SyntaxTree,
        val keyword: Token,
        val expression: ExpressionNode?
) : StatementNode(syntaxTree) {

    override val kind = SyntaxType.ReturnStatement
    override val children: Collection<Node> get() = arrayListOf<Node>(keyword).apply { expression?.let { add(it) } }
}