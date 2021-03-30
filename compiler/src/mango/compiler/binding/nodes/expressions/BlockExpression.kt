package mango.compiler.binding.nodes.expressions

import mango.compiler.binding.nodes.statements.Statement
import mango.compiler.symbols.TypeSymbol

class BlockExpression(
        val statements: Collection<Statement>,
        override val type: TypeSymbol,
        val isUnsafe: Boolean = false
) : Expression() {

    override val kind = Kind.BlockExpression
}
