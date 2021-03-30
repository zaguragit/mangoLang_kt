package mango.compiler.binding.nodes.expressions

import mango.compiler.symbols.TypeSymbol

class ErrorExpression : Expression() {
    override val type = TypeSymbol.err
    override val kind = Kind.ErrorExpression
}