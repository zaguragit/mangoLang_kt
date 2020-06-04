package mango.interpreter.binding

class BoundNopStatement : BoundStatement() {
    override val boundType = BoundNodeType.NopStatement
}