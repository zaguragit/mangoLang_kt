package mango.interpreter.symbols

import mango.interpreter.syntax.nodes.FunctionDeclarationNode

abstract class CallableSymbol(
    name: String,
    val parameters: Array<VariableSymbol>,
    returnType: TypeSymbol,
    val path: String,
    val declarationNode: FunctionDeclarationNode?,
    override val meta: MetaData
) : VariableSymbol(
    name,
    TypeSymbol.Fn(returnType, parameters.map { it.type }),
    true,
    null,
    Kind.Function,
    path
) {

    override val kind = Kind.Function

    inline val returnType get() = (type as TypeSymbol.Fn).returnType

    val suffix by lazy {
        generateSuffix(parameters.map { it.type }, meta.isExtension)
    }

    companion object {
        fun generateSuffix(parameters: List<TypeSymbol>, isExtension: Boolean) = buildString {
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