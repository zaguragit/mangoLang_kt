package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.Label

abstract class Loop(
    val breakLabel: Label,
    val continueLabel: Label
) : Statement()