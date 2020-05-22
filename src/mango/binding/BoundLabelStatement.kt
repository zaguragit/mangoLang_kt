package mango.binding

class BoundLabelStatement(
    val symbol: BoundLabel
) : BoundStatement() {

    override val boundType = BoundNodeType.LabelStatement
    override val children: Collection<BoundNode> get() = listOf()
}