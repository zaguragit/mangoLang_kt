package mango.interpreter.symbols

abstract class CallableSymbol : Symbol() {
    abstract val parameters: Array<VariableSymbol>

    val extra by lazy {
        generate(parameters.map { it.type }, meta.extensionType)
    }

    companion object {
        fun generate(parameters: List<TypeSymbol>, extensionType: TypeSymbol?) = buildString {
            append('[')
            for (p in parameters) {
                if (p !== parameters[0]) append(',')
                append(p.name)
            }
            if (extensionType != null) {
                append(']')
                append(extensionType.name)
            }
        }
    }
}