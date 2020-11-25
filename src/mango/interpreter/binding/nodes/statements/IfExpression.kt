package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.Expression
import mango.interpreter.symbols.TypeSymbol

class IfExpression(
    val condition: Expression,
    val thenExpression: Expression,
    val elseExpression: Expression?
) : Expression() {

    override val type: TypeSymbol
        get() = elseExpression?.type?.commonType(thenExpression.type) ?: TypeSymbol.Unit

    override val kind = Kind.IfExpression
}
