package mango.compiler.binding.nodes.expressions

import mango.compiler.symbols.TypeSymbol

class IfExpression(
        val condition: Expression,
        val thenExpression: Expression,
        val elseExpression: Expression?
) : Expression() {

    override val type: TypeSymbol
        get() = elseExpression?.type?.commonType(thenExpression.type) ?: TypeSymbol.Void

    override val kind = Kind.IfExpression
}
