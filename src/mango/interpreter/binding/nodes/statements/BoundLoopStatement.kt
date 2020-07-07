package mango.interpreter.binding.nodes.statements

import mango.interpreter.binding.BoundLabel

abstract class BoundLoopStatement(
        val breakLabel: BoundLabel,
        val continueLabel: BoundLabel
) : BoundStatement()