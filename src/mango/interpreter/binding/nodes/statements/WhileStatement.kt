package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.Label
import mango.interpreter.binding.nodes.expressions.BoundExpression

class WhileStatement(
        val condition: BoundExpression,
        val body: BlockStatement,
        breakLabel: Label,
        continueLabel: Label
) : LoopStatement(breakLabel, continueLabel) {

    override val kind
        get() = Kind.WhileStatement
}