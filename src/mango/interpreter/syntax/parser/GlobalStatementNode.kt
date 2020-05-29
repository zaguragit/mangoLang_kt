package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType

class GlobalStatementNode(
    val statement: StatementNode
) : MemberNode() {

    override val kind = SyntaxType.GlobalStatement
    override val children: Collection<Node> get() = listOf(statement)
}