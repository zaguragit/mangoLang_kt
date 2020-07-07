package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.VariableSymbol

class BoundVariableExpression(
    val variable: VariableSymbol
) : BoundExpression() {

    override val type = variable.type
    override val boundType = BoundNodeType.VariableExpression

    override fun toString() = variable.name

    override val constantValue get() = variable.constant
}
