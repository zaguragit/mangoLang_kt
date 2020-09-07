package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.Label
import mango.interpreter.binding.nodes.expressions.Expression

class WhileStatement(
    val condition: Expression,
    val body: BlockStatement,
    breakLabel: Label,
    continueLabel: Label
) : LoopStatement(breakLabel, continueLabel) {

    override val kind
        get() = Kind.WhileStatement
}