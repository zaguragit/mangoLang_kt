package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.BoundLabel
import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.nodes.expressions.BoundExpression

class BoundWhileStatement(
        val condition: BoundExpression,
        val body: BoundBlockStatement,
        breakLabel: BoundLabel,
        continueLabel: BoundLabel
) : BoundLoopStatement(breakLabel, continueLabel) {

    override val kind
        get() = BoundNodeType.WhileStatement
}