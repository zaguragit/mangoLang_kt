package mango.compiler.binding.nodes.statements

import mango.compiler.binding.nodes.expressions.Expression
import mango.compiler.ir.Label
import mango.compiler.symbols.VariableSymbol

class ForStatement(
        val variable: VariableSymbol,
        val lowerBound: Expression,
        val upperBound: Expression,
        val body: Statement,
        breakLabel: Label,
        continueLabel: Label
) : Loop(breakLabel, continueLabel) {

    override val kind = Kind.ForLoopStatement
}
