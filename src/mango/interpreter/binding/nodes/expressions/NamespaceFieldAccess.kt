package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.Namespace
import mango.interpreter.symbols.TypeSymbol

class NamespaceFieldAccess(
    val namespace: Namespace
) : Expression() {

    override val type get() = TypeSymbol.err
    override val kind = Kind.NamespaceFieldAccess

    override fun toString() = namespace.path
}
