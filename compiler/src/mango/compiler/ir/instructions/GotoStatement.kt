package mango.compiler.ir.instructions

import mango.compiler.binding.nodes.statements.Statement
import mango.compiler.ir.Label

class GotoStatement(
    val label: Label
) : Statement() {

    override val kind = Kind.GotoStatement
}