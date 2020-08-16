package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.BoundNodeType

class BoundBlockStatement(
    val statements: Collection<BoundStatement>
) : BoundStatement() {
    override val kind = BoundNodeType.BlockStatement
}
