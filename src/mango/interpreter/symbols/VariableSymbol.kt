package mango.interpreter.symbols

import mango.interpreter.binding.nodes.expressions.BoundConstant

open class VariableSymbol private constructor(
    override val name: String,
    override val type: TypeSymbol,
    val isReadOnly: Boolean,
    constant: BoundConstant?,
    override val kind: Kind,
    var realName: String
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
            Kind.Parameter,
            ".p_$name"
        )

        fun visible(
            name: String,
            type: TypeSymbol,
            isReadOnly: Boolean,
            constant: BoundConstant?,
            path: String
        ): VariableSymbol = object : VariableSymbol(
            name, type, isReadOnly, constant, Kind.VisibleVariable, path
        ), VisibleSymbol {
            override val path get() = realName
        }

        fun local(
            name: String,
            type: TypeSymbol,
            isReadOnly: Boolean,
            constant: BoundConstant?
        ) = VariableSymbol(name, type, isReadOnly, constant, Kind.Variable, name)
    }
}