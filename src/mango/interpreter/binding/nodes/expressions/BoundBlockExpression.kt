package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.nodes.statements.BoundStatement
import mango.interpreter.symbols.TypeSymbol

class BoundBlockExpression(
    val statements: Collection<BoundStatement>,
    override val type: TypeSymbol
) : BoundExpression() {

    override val boundType = BoundNodeType.BlockExpression
}
