package mango.binding

class BoundVariableDeclaration(
        val variable: VariableSymbol,
        val initializer: BoundExpression
) : BoundStatement() {

    override val boundType = BoundNodeType.VariableDeclaration
    override val children get() = listOf(initializer)

    override fun getDataString() = variable.name
}