package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.BoundNamespace
import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.TypeSymbol

class BoundNamespaceFieldAccess(
    val namespace: BoundNamespace
) : BoundExpression() {

    override val type get() = TypeSymbol.err
    override val kind = BoundNodeType.NamespaceFieldAccess

    override fun toString() = namespace.path
}
