package mango.interpreter.binding

import mango.interpreter.symbols.TypeSymbol

abstract class BoundExpression : BoundNode() {
    abstract val type: TypeSymbol
    open val constantValue: BoundConstant? = null
}

class BoundConstant(val value: Any?)