package mango.binding

class BoundGotoStatement(
    val label: BoundLabel
) : BoundStatement() {

    override val boundType = BoundNodeType.GotoStatement
    override val children: Collection<BoundNode> get() = listOf()
}