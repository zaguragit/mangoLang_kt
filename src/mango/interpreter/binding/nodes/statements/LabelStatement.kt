package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.Label

class LabelStatement(
    val symbol: Label
) : Statement() {

    override val kind = Kind.LabelStatement
}