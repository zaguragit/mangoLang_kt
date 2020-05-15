package mango.binding

class BoundIfStatement(
    val condition: BoundExpression,
    val statement: BoundBlockStatement,
    val elseStatement: BoundStatement?
) : BoundStatement() {

    override val boundType = BoundNodeType.IfStatement
    override val children: Collection<BoundNode>
        get() = arrayListOf(condition, statement).apply {
            if (elseStatement != null) {
                add(elseStatement)
            }
        }
}
