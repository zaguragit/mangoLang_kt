package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.BoundLabel
import mango.interpreter.binding.nodes.BoundNodeType

class BoundLabelStatement(
    val symbol: BoundLabel
) : BoundStatement() {

    override val boundType = BoundNodeType.LabelStatement
}