package mango.binding

class BoundIfStatement(
    val condition: BoundExpression,
    val statement: BoundBlockStatement,
    val elseStatement: BoundStatement?
) : BoundStatement() {

    override val boundType = BoundNodeType.IfStatement
}
