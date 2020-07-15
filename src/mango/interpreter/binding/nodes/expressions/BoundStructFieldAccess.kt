package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.symbols.VariableSymbol

class BoundStructFieldAccess(
    val struct: BoundExpression,
    val i: Int
) : BoundExpression() {

    inline val field get() = (struct.type as TypeSymbol.StructTypeSymbol).fields[i]

    override val type = field.type
    override val boundType = BoundNodeType.StructFieldAccess

    override fun toString() = field.name
}
