package mango.binding

class BoundVariableExpression(
    val variable: VariableSymbol
) : BoundExpression() {

    override val type = variable.type
    override val boundType = BoundNodeType.VariableExpression
    override val children get() = listOf<BoundNode>()

    override fun getDataString() = variable.name
}
