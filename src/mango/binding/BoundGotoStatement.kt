package mango.binding

class BoundGotoStatement(
    val label: BoundLabel
) : BoundStatement() {

    override val boundType = BoundNodeType.GotoStatement
}