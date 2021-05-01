package mango.cli.emission.headers

import mango.cli.emission.Emitter
import mango.compiler.binding.Namespace
import mango.compiler.binding.Program
import mango.compiler.binding.nodes.statements.Statement
import mango.compiler.binding.structureString
import mango.compiler.symbols.CallableSymbol
import mango.compiler.symbols.Symbol
import mango.compiler.symbols.TypeSymbol
import mango.compiler.symbols.VariableSymbol

object HeaderEmitter : Emitter {

    override fun emit(
        program: Program,
        moduleName: String
    ): String {
        return emitNamespace(Namespace[moduleName]!!, program).contentString()
    }

    fun emitNamespace(namespace: Namespace, program: Program): Header.Namespace {
        val namespaceRepresentation = Header.Namespace(namespace)
        for (symbol in namespace.symbols) {
            if (!symbol.meta.isInternal) {
                when (symbol.kind) {
                    Symbol.Kind.Function -> {
                        symbol as CallableSymbol
                        val function = if (symbol.meta.isInline) {
                            Header.InlineFunction(symbol, program.functionBodies!![symbol]!!, program.functionBodies!!)
                        } else Header.Function(symbol)
                        namespaceRepresentation.add(function)
                    }
                    Symbol.Kind.StructType -> {
                        symbol as TypeSymbol.StructTypeSymbol
                        namespaceRepresentation.add(Header.Type(symbol))
                    }
                }
            }
        }
        for (child in Namespace.all) {
            if (child !== namespace && child.parent === namespace) {
                namespaceRepresentation.add(emitNamespace(child, program))
            }
        }
        return namespaceRepresentation
    }

    interface Header {

        class Function (val symbol: CallableSymbol) : Header {
            override fun toString () = "\n@cname(\"${symbol.mangledName()}\")\nval " + run {
                val a = ArrayList<VariableSymbol>()
                a.addAll(symbol.parameters)
                val b = if (symbol.meta.isExtension) {
                    a.removeAt(0)
                    val p = symbol.parameters[0]
                    "(${p.name} ${p.type}) ${symbol.name}"
                } else symbol.name
                b + a.joinToString(separator = ", ", prefix = " (", postfix = ")") {
                    it.name + ' ' + it.type
                }
            } + ' ' + symbol.returnType.path
        }

        class InlineFunction (val symbol: CallableSymbol, val body: Statement, val functionBodies: HashMap<CallableSymbol, Statement?>) : Header {
            override fun toString (): String {
                return "\n@inline\nval " + run {
                    val a = ArrayList<VariableSymbol>()
                    a.addAll(symbol.parameters)
                    val b = if (symbol.meta.isExtension) {
                        a.removeAt(0)
                        val p = symbol.parameters[0]
                        "(${p.name} ${p.type}) ${symbol.name}"
                    } else symbol.name
                    b + a.joinToString(separator = ", ", prefix = " (", postfix = ")") {
                        it.name + ' ' + it.type
                    }
                } + ' ' + symbol.returnType.path + " -> " + body.structureString(functionBodies)
            }
        }

        class Namespace (namespace: mango.compiler.binding.Namespace) : Header {

            val name = namespace.path.substringAfterLast('.')
            val used = namespace.used

            private val content = ArrayList<Header>()

            fun add (header: Header) = content.add(header)

            fun contentString() = content.joinToString(separator = "\n", prefix = "\n")

            override fun toString () = "namespace $name {" + (
                    used.joinToString(separator = "\n", prefix = "\n") +
                    contentString()
                ).replace("\n", "\n\t") +
                "\n}\n\n"
        }

        class Type (val symbol: TypeSymbol.StructTypeSymbol) : Header {

            override fun toString () = "type ${if (symbol.parentType == TypeSymbol.Any) symbol.name else symbol.name + " : ${symbol.parentType!!.name}"} {" +
                symbol.fields.joinToString(separator = "\n", prefix = "\n").replace("\n", "\n\t") + "\n}\n\n"
        }
    }
}