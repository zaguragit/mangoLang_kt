package mango.interpreter.binding

class BoundBlockStatement(
    val statements: Collection<BoundStatement>
) : BoundStatement() {
    override val boundType = BoundNodeType.BlockStatement
}
