package mango.compiler.binding.nodes.expressions

import mango.compiler.symbols.TypeSymbol

class Reference(
    val expression: NameExpression
) : Expression() {

    override val type = TypeSymbol.Ptr(arrayOf(expression.type))
    override val kind = Kind.ReferenceExpression
}
