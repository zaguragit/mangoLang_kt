package mango.binding

import mango.symbols.TypeSymbol

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
            if (from == TypeSymbol.int || from == TypeSymbol.bool) {
                if (to == TypeSymbol.string) {
                    return Explicit
                }
            }
            if (from == TypeSymbol.string) {
                if (to == TypeSymbol.int || to == TypeSymbol.bool) {
                    return Explicit
                }
            }
            return None
        }
    }
}