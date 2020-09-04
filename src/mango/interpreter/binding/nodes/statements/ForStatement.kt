package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.Label
import mango.interpreter.binding.nodes.expressions.BoundExpression
import mango.interpreter.symbols.VariableSymbol

class ForStatement(
        val variable: VariableSymbol,
        val lowerBound: BoundExpression,
        val upperBound: BoundExpression,
        val body: BlockStatement,
        breakLabel: Label,
        continueLabel: Label
) : LoopStatement(breakLabel, continueLabel) {

    override val kind = Kind.ForStatement
}
