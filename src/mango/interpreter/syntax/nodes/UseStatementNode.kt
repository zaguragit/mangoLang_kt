package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.SyntaxTree

class UseStatementNode(
        syntaxTree: SyntaxTree,
        val keyword: Token,
        val directories: Collection<Token>,
        val isInclude: Boolean
) : TopLevelNode(syntaxTree) {

    override val kind = SyntaxType.UseStatement
    override val children: Collection<Node> get() = arrayListOf(keyword).apply { addAll(directories) }
}