package mango.compiler.binding.nodes.statements

import mango.compiler.binding.nodes.BoundNode

abstract class Statement : BoundNode()

object NopStatement : Statement() {
    override val kind = Kind.NopStatement
}