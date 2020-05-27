package mango.binding

class BoundLabelStatement(
    val symbol: BoundLabel
) : BoundStatement() {

    override val boundType = BoundNodeType.LabelStatement
}