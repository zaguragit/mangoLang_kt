package mango.interpreter.binding

import mango.interpreter.symbols.VariableSymbol

class BoundForStatement(
    val variable: VariableSymbol,
    val lowerBound: BoundExpression,
    val upperBound: BoundExpression,
    val body: BoundBlockStatement,
    breakLabel: BoundLabel,
    continueLabel: BoundLabel
) : BoundLoopStatement(breakLabel, continueLabel) {

    override val boundType = BoundNodeType.ForStatement
}
