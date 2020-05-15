package mango.binding

class BoundBlockStatement(
    val statements: Collection<BoundStatement>
) : BoundStatement() {
    override val boundType = BoundNodeType.BlockStatement
    override val children = statements
}
