package mango.binding

class BoundForStatement(
    val variable: VariableSymbol,
    val lowerBound: BoundExpression,
    val upperBound: BoundExpression,
    val body: BoundBlockStatement
) : BoundStatement() {

    override val boundType = BoundNodeType.ForStatement
    override val children: Collection<BoundNode> get() = listOf(lowerBound, upperBound, body)
}
