package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.CallableSymbol

class BoundCallExpression(
    val symbol: CallableSymbol,
    val arguments: Collection<BoundExpression>
) : BoundExpression() {

    override
    val type get() = symbol.returnType

    override
    val kind = BoundNodeType.CallExpression
}
