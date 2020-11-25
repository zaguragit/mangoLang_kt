package mango.interpreter.symbols

import mango.interpreter.syntax.nodes.FunctionDeclarationNode

open class CallableSymbol(
    name: String,
    val parameters: Array<VariableSymbol>,
    override val type: TypeSymbol.Fn,
    path: String,
    val declarationNode: FunctionDeclarationNode?,
    override val meta: MetaData
) : VariableSymbol(
    name,
    type,
    true,
    null,
    Kind.Function,
    path
), VisibleSymbol {

    override val path get() = realName

    inline val returnType get() = type.returnType

    val suffix by lazy {
        generateSuffix(type.args, meta.isExtension)
    }

    companion object {
        fun generateSuffix(parameters: Collection<TypeSymbol>, isExtension: Boolean) = buildString {
            if (parameters.isNotEmpty()) {
                append('<')
                for (i in parameters.indices) {
                    if (i != 0) append(',')
                    else if (isExtension) {
                        append('$')
                    }
                    append(parameters.elementAt(i).toString())
                }
                append('>')
            }
        }
    }
}