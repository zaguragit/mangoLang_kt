package mango.interpreter.binding

abstract class BoundLoopStatement(
    val breakLabel: BoundLabel,
    val continueLabel: BoundLabel
) : BoundStatement()