package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.expressions.Expression
import mango.interpreter.symbols.VariableSymbol

class VariableDeclaration(
    val variable: VariableSymbol,
    val initializer: Expression
) : Statement() {

    override val kind = Kind.VariableDeclaration
}