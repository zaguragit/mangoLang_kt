package mango.binding

class BoundConditionalGotoStatement(
    val label: LabelSymbol,
    val condition: BoundExpression,
    val jumpIfTrue: Boolean
) : BoundStatement() {

    override val boundType = BoundNodeType.ConditionalGotoStatement
    override val children: Collection<BoundNode> get() = listOf(condition)
    override fun getDataString() = label.name
}