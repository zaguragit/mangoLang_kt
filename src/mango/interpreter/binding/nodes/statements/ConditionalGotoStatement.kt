package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.Label
import mango.interpreter.binding.nodes.expressions.Expression

class ConditionalGotoStatement(
        val label: Label,
        val condition: Expression,
        val jumpIfTrue: Boolean
) : Statement() {

    override val kind = Kind.ConditionalGotoStatement
}