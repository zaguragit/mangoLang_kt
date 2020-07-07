package mango.interpreter.symbols

import mango.interpreter.binding.nodes.expressions.BoundConstant

open class VariableSymbol private constructor(
    override val name: String,
    val type: TypeSymbol,
    val isReadOnly: Boolean,
    constant: BoundConstant?,
    override val kind: Kind
) : Symbol() {

    val constant =
        if (isReadOnly) { constant }
        else { null }

    companion object {

        fun param(name: String, type: TypeSymbol) = VariableSymbol(
            name,
            type,
            true,
            null,
            Kind.Parameter
        )

        fun visible(
            name: String,
            type: TypeSymbol,
            isReadOnly: Boolean,
            constant: BoundConstant?,
            path: String
        ): VariableSymbol = object : VariableSymbol(
            name, type, isReadOnly, constant, Kind.VisibleVariable
        ), VisibleSymbol {
            override val path = path
        }

        fun local(
            name: String,
            type: TypeSymbol,
            isReadOnly: Boolean,
            constant: BoundConstant?
        ) = VariableSymbol(name, type, isReadOnly, constant, Kind.Variable)
    }
}