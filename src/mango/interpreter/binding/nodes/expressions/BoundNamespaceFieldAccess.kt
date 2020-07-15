package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.BoundNamespace
import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.Symbol
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.symbols.VariableSymbol

class BoundNamespaceFieldAccess(
    val namespace: BoundNamespace
) : BoundExpression() {

    override val type get() = TypeSymbol.err
    override val boundType = BoundNodeType.NamespaceFieldAccess

    override fun toString() = namespace.path
}
