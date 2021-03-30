package mango.compiler.ir.instructions

import mango.compiler.binding.nodes.statements.Statement
import mango.compiler.ir.Label

class LabelStatement(
    val symbol: Label
) : Statement() {

    override val kind = Kind.LabelStatement
}