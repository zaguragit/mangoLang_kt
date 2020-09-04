package mango.interpreter.binding.nodes.expressions

import mango.interpreter.symbols.TypeSymbol

class CallExpression(
    val expression: BoundExpression,
    val arguments: Collection<BoundExpression>
) : BoundExpression() {

    override val type get() = (expression.type as TypeSymbol.Fn).returnType
    override val kind = Kind.CallExpression
}
