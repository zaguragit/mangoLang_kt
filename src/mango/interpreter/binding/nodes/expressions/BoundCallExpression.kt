package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.CallableSymbol
import mango.interpreter.symbols.TypeSymbol

class BoundCallExpression(
    val expression: BoundExpression,
    val arguments: Collection<BoundExpression>
) : BoundExpression() {

    override val type get() = (expression.type as TypeSymbol.Fn).returnType
    override val kind = BoundNodeType.CallExpression
}
