package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.BoundExpression
import mango.interpreter.symbols.VariableSymbol

class VariableDeclaration(
    val variable: VariableSymbol,
    val initializer: BoundExpression
) : Statement() {

    override val kind = Kind.VariableDeclaration
}