package mango.interpreter.binding.nodes.expressions

import mango.interpreter.symbols.TypeSymbol

class PointerArrayInitialization(
    override val type: TypeSymbol,
    val length: Expression?,
    val expressions: List<Expression>?
) : Expression() {

    override val kind = Kind.PointerArrayInitialization
}