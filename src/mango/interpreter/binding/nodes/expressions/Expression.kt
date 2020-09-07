package mango.interpreter.binding.nodes.expressions

import mango.interpreter.binding.nodes.BoundNode
import mango.interpreter.symbols.TypeSymbol

abstract class Expression : BoundNode() {
    abstract val type: TypeSymbol
    open val constantValue: BoundConstant? = null
}

class BoundConstant(val value: Any?)