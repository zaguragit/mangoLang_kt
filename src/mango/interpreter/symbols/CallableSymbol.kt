package mango.interpreter.symbols

abstract class CallableSymbol : Symbol() {

    inline val returnType get() = type.returnType

    abstract override val type: TypeSymbol.Fn
    abstract val parameters: Array<VariableSymbol>

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