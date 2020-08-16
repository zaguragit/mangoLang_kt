package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.BoundLabel
import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.nodes.expressions.BoundExpression
import mango.interpreter.symbols.VariableSymbol

class BoundForStatement(
        val variable: VariableSymbol,
        val lowerBound: BoundExpression,
        val upperBound: BoundExpression,
        val body: BoundBlockStatement,
        breakLabel: BoundLabel,
        continueLabel: BoundLabel
) : BoundLoopStatement(breakLabel, continueLabel) {

    override val kind = BoundNodeType.ForStatement
}
