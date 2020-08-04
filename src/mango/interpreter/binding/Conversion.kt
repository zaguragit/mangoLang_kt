package mango.interpreter.binding

import mango.interpreter.symbols.Symbol
import mango.interpreter.symbols.TypeSymbol

enum class Conversion(
    val exists: Boolean,
    val isIdentity: Boolean,
    val isImplicit: Boolean
) {

    Identity(true, true, true),
    Implicit(true, false, true),
    Explicit(true, false, false),
    None(false, false, false);

    companion object {
        fun classify(from: TypeSymbol, to: TypeSymbol): Conversion {
            return when {
                from == to -> Identity
                from.isOfType(to) -> Implicit
                from.kind == Symbol.Kind.Struct && to.kind == Symbol.Kind.Struct -> Explicit
                else -> None
            }
        }
    }
}