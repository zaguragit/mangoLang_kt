package mango.compiler.binding.nodes.statements

import mango.compiler.binding.nodes.expressions.Expression
import mango.compiler.ir.Label

class ConditionalGoto(
    val label: Label,
    val condition: Expression,
    val jumpIfTrue: Boolean
) : Statement() {

    override val kind = Kind.ConditionalGotoStatement
}