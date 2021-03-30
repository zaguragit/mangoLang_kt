package mango.compiler.binding.nodes.expressions

import mango.compiler.binding.Namespace
import mango.compiler.symbols.TypeSymbol

class NamespaceFieldAccess(
    val namespace: Namespace
) : Expression() {

    override val type get() = TypeSymbol.err
    override val kind = Kind.NamespaceFieldAccess

    override fun toString() = namespace.path
}
