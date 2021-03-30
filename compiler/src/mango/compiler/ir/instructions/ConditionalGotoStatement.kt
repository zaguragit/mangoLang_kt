package mango.compiler.ir.instructions

import mango.compiler.binding.nodes.expressions.Expression
import mango.compiler.binding.nodes.statements.Statement
import mango.compiler.ir.Label

class ConditionalGotoStatement(
    val label: Label,
    val condition: Expression,
    val jumpIfTrue: Boolean
) : Statement() {

    override val kind = Kind.ConditionalGotoStatement
}