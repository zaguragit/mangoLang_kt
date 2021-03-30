package mango.compiler.binding.nodes.expressions

class PointerAccess(
        val expression: Expression,
        val i: Expression
) : Expression() {

    override val type = expression.type.params[0]
    override val kind = Kind.PointerAccessExpression
}
