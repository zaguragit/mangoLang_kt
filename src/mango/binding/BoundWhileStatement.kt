package mango.binding

class BoundWhileStatement(
    val condition: BoundExpression,
    val body: BoundBlockStatement
) : BoundStatement() {

    override val boundType
        get() = BoundNodeType.WhileStatement
    override val children: Collection<BoundNode>
        get() = listOf(condition, body)
}