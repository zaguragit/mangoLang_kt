package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.Label

class LoopStatement(
    val body: Statement,
    breakLabel: Label,
    continueLabel: Label
) : Loop(breakLabel, continueLabel) {

    override val kind
        get() = Kind.WhileStatement
}