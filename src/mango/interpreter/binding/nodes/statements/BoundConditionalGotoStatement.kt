package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.BoundExpression
import mango.interpreter.binding.BoundLabel
import mango.interpreter.binding.nodes.BoundNodeType

class BoundConditionalGotoStatement(
        val label: BoundLabel,
        val condition: BoundExpression,
        val jumpIfTrue: Boolean
) : BoundStatement() {

    override val boundType = BoundNodeType.ConditionalGotoStatement
}