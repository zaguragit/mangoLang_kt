package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNode
import mango.interpreter.symbols.TypeSymbol

abstract class Expression : BoundNode() {
    abstract val type: TypeSymbol
    open val constantValue: BoundConstant? = null

    inline fun isError() = type == TypeSymbol.err
    inline fun isNotError() = !isError()
}

class BoundConstant(val value: Any?)