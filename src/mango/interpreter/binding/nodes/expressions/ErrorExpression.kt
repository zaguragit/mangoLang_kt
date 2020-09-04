package mango.interpreter.binding.nodes.expressions

import mango.interpreter.symbols.TypeSymbol

class ErrorExpression : BoundExpression() {
    override val type = TypeSymbol.err
    override val kind = Kind.ErrorExpression
}