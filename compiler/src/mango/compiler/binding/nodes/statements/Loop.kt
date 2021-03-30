package mango.compiler.binding.nodes.statements

import mango.compiler.ir.Label

abstract class Loop(
    val breakLabel: Label,
    val continueLabel: Label
) : Statement()