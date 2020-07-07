package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.FunctionSymbol

class BoundCallExpression(
    val function: FunctionSymbol,
    val arguments: Collection<BoundExpression>
) : BoundExpression() {

    override val type get() = function.type
    override val boundType = BoundNodeType.CallExpression
}
