package mango.compiler.binding.nodes.expressions

import mango.compiler.symbols.TypeSymbol

class StructInitialization(
    override val type: TypeSymbol.StructTypeSymbol,
    val fields: Map<TypeSymbol.StructTypeSymbol.Field, Expression>
) : Expression() {

    override val kind = Kind.StructInitialization
}
