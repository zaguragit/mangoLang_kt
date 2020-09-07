package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.Expression
import mango.interpreter.symbols.VariableSymbol

class Assignment(
    val variable: VariableSymbol,
    val expression: Expression
) : Statement() {

    override val kind = Kind.AssignmentStatement
}
