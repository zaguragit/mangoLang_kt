package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.BoundLabel
import mango.interpreter.binding.nodes.BoundNodeType

class BoundGotoStatement(
    val label: BoundLabel
) : BoundStatement() {

    override val kind = BoundNodeType.GotoStatement
}