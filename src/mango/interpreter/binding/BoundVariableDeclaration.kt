package mango.interpreter.binding

import mango.interpreter.symbols.VariableSymbol

class BoundVariableDeclaration(
    val variable: VariableSymbol,
    val initializer: BoundExpression
) : BoundStatement() {

    override val boundType = BoundNodeType.VariableDeclaration
}