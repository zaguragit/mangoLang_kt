package mango.binding

class BoundGotoStatement(
    val label: LabelSymbol
) : BoundStatement() {

    override val boundType = BoundNodeType.GotoStatement
    override val children: Collection<BoundNode> get() = listOf()
    override fun getDataString() = label.name
}