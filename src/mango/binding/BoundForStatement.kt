package mango.binding

import mango.symbols.VariableSymbol

class BoundForStatement(
    val variable: VariableSymbol,
    val lowerBound: BoundExpression,
    val upperBound: BoundExpression,
    val body: BoundBlockStatement,
    breakLabel: BoundLabel,
    continueLabel: BoundLabel
) : BoundLoopStatement(breakLabel, continueLabel) {

    override val boundType = BoundNodeType.ForStatement
    override val children: Collection<BoundNode> get() = listOf(lowerBound, upperBound, body)
}
