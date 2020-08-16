package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.TypeSymbol

class BoundReference(
    val expression: BoundVariableExpression
) : BoundExpression() {

    override val type = TypeSymbol.Ptr(arrayOf(expression.type))
    override val kind = BoundNodeType.ReferenceExpression
}
