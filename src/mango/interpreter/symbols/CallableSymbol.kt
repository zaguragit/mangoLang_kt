package mango.interpreter.symbols

import mango.interpreter.syntax.nodes.FunctionDeclarationNode
import java.util.*

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
        Info(this).toString()
    }

    class Info {
        val parameters: Collection<TypeSymbol>
        val isExtension: Boolean

        constructor(parameters: Collection<TypeSymbol>, isExtension: Boolean) {
            this.parameters = parameters
            this.isExtension = isExtension
        }

        constructor(symbol: CallableSymbol) {
            this.parameters = symbol.type.args
            this.isExtension = symbol.meta.isExtension
        }

        override fun toString() = buildString {
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

        override fun hashCode() = Objects.hash(parameters, isExtension)

        fun matches(type: TypeSymbol.Fn, isExtension: Boolean): Boolean {
            if (this.isExtension != isExtension) {
                return false
            }
            if (parameters.size != type.args.size) return false
            parameters.forEachIndexed { i, p ->
                if (!p.isOfType(type.args[i])) {
                    return false
                }
            }
            return true
        }
    }
}