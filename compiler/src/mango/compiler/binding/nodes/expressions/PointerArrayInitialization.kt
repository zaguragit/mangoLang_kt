package mango.compiler.binding.nodes.expressions

import mango.compiler.symbols.TypeSymbol

class PointerArrayInitialization(
    override val type: TypeSymbol,
    val length: Expression?,
    val expressions: List<Expression>?
) : Expression() {

    override val kind = Kind.PointerArrayInitialization
}