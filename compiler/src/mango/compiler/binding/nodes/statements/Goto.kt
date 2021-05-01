package mango.compiler.binding.nodes.statements

import mango.compiler.ir.Label

class Goto(
    val label: Label,
    val type: Type = Type.Jump
) : Statement() {

    override val kind = Kind.GotoStatement

    enum class Type {
        Return, Break, Continue, Jump
    }
}