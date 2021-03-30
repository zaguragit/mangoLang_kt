package mango.compiler.binding.nodes.expressions

import mango.compiler.symbols.VariableSymbol

class NameExpression(
    val symbol: VariableSymbol
) : Expression() {

    override val type = symbol.type
    override val kind = Kind.NameExpression

    override fun toString() = symbol.name

    override val constantValue get() = symbol.constant
}
