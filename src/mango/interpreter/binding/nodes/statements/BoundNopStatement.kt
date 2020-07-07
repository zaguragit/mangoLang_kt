package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.BoundNodeType

class BoundNopStatement : BoundStatement() {
    override val boundType = BoundNodeType.NopStatement
}