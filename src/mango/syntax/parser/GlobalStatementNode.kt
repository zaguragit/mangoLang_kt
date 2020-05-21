package mango.syntax.parser

import mango.syntax.SyntaxType

class GlobalStatementNode(
    val statement: StatementNode
) : MemberNode() {

    override val kind = SyntaxType.GlobalStatement
    override val children: Collection<Node> get() = listOf(statement)
}