package mango.compiler.binding.nodes.expressions

import mango.compiler.binding.nodes.BoundNode
import mango.compiler.symbols.TypeSymbol

abstract class Expression : BoundNode() {
    abstract val type: TypeSymbol
    open val constantValue: BoundConstant? = null

    inline fun isError() = type == TypeSymbol.err
    inline fun isNotError() = !isError()
}

class BoundConstant(val value: Any?)