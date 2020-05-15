package mango.binding

class BoundAssignmentExpression(
        val variable: VariableSymbol,
        val expression: BoundExpression
) : BoundExpression() {

    override val type get() = expression.type
    override val boundType = BoundNodeType.AssignmentExpression
    override val children get() = listOf(expression)

    override fun getDataString() = variable.name
}
