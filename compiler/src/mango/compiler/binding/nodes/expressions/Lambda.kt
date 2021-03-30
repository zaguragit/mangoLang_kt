package mango.compiler.binding.nodes.expressions

import mango.compiler.symbols.CallableSymbol
import mango.compiler.symbols.TypeSymbol

class Lambda(
    val symbol: CallableSymbol
) : Expression() {

    override val type: TypeSymbol.Fn get() = symbol.type

    override val kind = Kind.Lambda
}