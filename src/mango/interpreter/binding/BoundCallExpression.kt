package mango.interpreter.binding

import mango.interpreter.symbols.FunctionSymbol

class BoundCallExpression(
    val function: FunctionSymbol,
    val arguments: Collection<BoundExpression>
) : BoundExpression() {

    override val type get() = function.type
    override val boundType = BoundNodeType.CallExpression
}
