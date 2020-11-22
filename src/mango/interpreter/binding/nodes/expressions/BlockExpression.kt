package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.statements.Statement
import mango.interpreter.symbols.TypeSymbol

class BlockExpression(
    val statements: Collection<Statement>,
    override val type: TypeSymbol,
    val isUnsafe: Boolean = false
) : Expression() {

    override val kind = Kind.BlockExpression
}
