package mango.interpreter.binding.nodes.statements

class BlockStatement(
    val statements: Collection<Statement>
) : Statement() {
    override val kind = Kind.BlockStatement
}
