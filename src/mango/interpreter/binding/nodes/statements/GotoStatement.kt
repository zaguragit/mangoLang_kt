package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.Label

class GotoStatement(
    val label: Label
) : Statement() {

    override val kind = Kind.GotoStatement
}