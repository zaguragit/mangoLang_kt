package mango.compiler.binding.nodes.statements

import mango.compiler.ir.Label

class LabelStatement(
    val symbol: Label
) : Statement() {

    override val kind = Kind.LabelStatement
}