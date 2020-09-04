package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.BoundExpression
import mango.interpreter.binding.Label

class ConditionalGotoStatement(
        val label: Label,
        val condition: BoundExpression,
        val jumpIfTrue: Boolean
) : Statement() {

    override val kind = Kind.ConditionalGotoStatement
}