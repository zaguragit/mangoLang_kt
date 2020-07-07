package mango.interpreter.binding

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
            if (from == to) {
                return Identity
            }
            if (from == TypeSymbol.Int || from == TypeSymbol.Bool) {
                if (to == TypeSymbol.String) {
                    return Explicit
                }
            }
            if (from == TypeSymbol.String) {
                if (to == TypeSymbol.Int || to == TypeSymbol.Bool) {
                    return Explicit
                }
            }
            return None
        }
    }
}