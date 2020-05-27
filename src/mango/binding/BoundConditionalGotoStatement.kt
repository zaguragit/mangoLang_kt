package mango.binding

class BoundConditionalGotoStatement(
        val label: BoundLabel,
        val condition: BoundExpression,
        val jumpIfTrue: Boolean
) : BoundStatement() {

    override val boundType = BoundNodeType.ConditionalGotoStatement
}