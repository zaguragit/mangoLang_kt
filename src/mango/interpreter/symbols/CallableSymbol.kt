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
            for (p in parameters) {
                append('[')
                append(p.name)
            }
            if (isExtension) {
                append('[')
            }
        }
    }
}