package mango.binding

class BoundWhileStatement(
    val condition: BoundExpression,
    val body: BoundBlockStatement,
    breakLabel: BoundLabel,
    continueLabel: BoundLabel
) : BoundLoopStatement(breakLabel, continueLabel) {

    override val boundType
        get() = BoundNodeType.WhileStatement
    override val children: Collection<BoundNode>
        get() = listOf(condition, body)
}