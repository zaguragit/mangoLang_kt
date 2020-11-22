package mango.interpreter.binding.nodes.expressions

import mango.interpreter.symbols.TypeSymbol

class CallExpression(
    val expression: Expression,
    val arguments: Collection<Expression>,
    val isExtension: Boolean = false
) : Expression() {

    override val type get() = (expression.type as TypeSymbol.Fn).returnType
    override val kind = Kind.CallExpression
}
