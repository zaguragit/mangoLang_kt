package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType

class GlobalStatementNode(
    syntaxTree: SyntaxTree,
    val statement: StatementNode
) : MemberNode(syntaxTree) {

    override val kind = SyntaxType.GlobalStatement
    override val children: Collection<Node> get() = listOf(statement)
}