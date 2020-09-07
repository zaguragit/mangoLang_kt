package mango.interpreter.binding.nodes.expressions

import mango.interpreter.symbols.TypeSymbol

class ErrorExpression : Expression() {
    override val type = TypeSymbol.err
    override val kind = Kind.ErrorExpression
}