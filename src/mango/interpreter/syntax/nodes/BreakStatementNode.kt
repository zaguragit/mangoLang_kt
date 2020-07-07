package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class BreakStatementNode(
        syntaxTree: SyntaxTree,
        val keyword: Token
) : StatementNode(syntaxTree) {

    override val kind = SyntaxType.BreakStatement
    override val children: Collection<Node> get() = listOf(keyword)
}
