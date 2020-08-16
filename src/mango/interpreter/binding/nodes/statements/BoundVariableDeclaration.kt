package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.nodes.expressions.BoundExpression
import mango.interpreter.symbols.VariableSymbol

class BoundVariableDeclaration(
    val variable: VariableSymbol,
    val initializer: BoundExpression
) : BoundStatement() {

    override val kind = BoundNodeType.VariableDeclaration
}