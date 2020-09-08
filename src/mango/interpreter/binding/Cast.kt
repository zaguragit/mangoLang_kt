package mango.interpreter.binding

import mango.interpreter.symbols.Symbol
import mango.interpreter.symbols.TypeSymbol

enum class Cast(
    val exists: Boolean,
    val isIdentity: Boolean,
    val isImplicit: Boolean
) {

    Identity(true, true, true),
    Implicit(true, false, true),
    Explicit(true, false, false),
    None(false, false, false);

    companion object {
        fun classify(from: TypeSymbol, to: TypeSymbol): Cast {
            return when {
                from == to -> Identity
                from.isOfType(to) -> Implicit
                from.kind == Symbol.Kind.StructType && to.kind == Symbol.Kind.StructType -> Explicit
                else -> None
            }
        }
    }
}