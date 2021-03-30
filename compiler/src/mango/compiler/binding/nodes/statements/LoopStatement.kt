package mango.compiler.binding.nodes.statements

import mango.compiler.ir.Label

class LoopStatement(
    val body: Statement,
    breakLabel: Label,
    continueLabel: Label
) : Loop(breakLabel, continueLabel) {

    override val kind
        get() = Kind.LoopStatement
}