package mango.compiler.binding.nodes.expressions

import mango.compiler.binding.nodes.statements.ExpressionStatement
import mango.compiler.binding.nodes.statements.Statement
import mango.compiler.symbols.TypeSymbol

class BlockExpression(
    val statements: Collection<Statement>,
    override val type: TypeSymbol,
    val isUnsafe: Boolean = false
) : Expression() {

    override val kind = Kind.BlockExpression

    override val constantValue: BoundConstant?
        get() = statements.lastOrNull().let {
            if (it?.kind == Kind.ExpressionStatement) (it as ExpressionStatement).expression.constantValue else null
        }
}
