package mango.binding

class BoundLabelStatement(
    val symbol: LabelSymbol
) : BoundStatement() {

    override val boundType = BoundNodeType.LabelStatement
    override val children: Collection<BoundNode> get() = listOf()
    override fun getDataString() = symbol.name
}