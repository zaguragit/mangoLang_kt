package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.Label
import mango.interpreter.binding.nodes.expressions.Expression
import mango.interpreter.symbols.VariableSymbol

class ForStatement(
    val variable: VariableSymbol,
    val lowerBound: Expression,
    val upperBound: Expression,
    val body: BlockStatement,
    breakLabel: Label,
    continueLabel: Label
) : LoopStatement(breakLabel, continueLabel) {

    override val kind = Kind.ForStatement
}
