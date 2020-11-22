package mango.compilation.headers

import mango.compilation.Emitter
import mango.interpreter.binding.Namespace
import mango.interpreter.binding.Program
import mango.interpreter.binding.nodes.statements.Statement
import mango.interpreter.symbols.CallableSymbol
import mango.interpreter.symbols.Symbol
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.symbols.VariableSymbol

object HeaderEmitter : Emitter {

    override fun emit(
        program: Program,
        moduleName: String
    ): String {
        val namespaceMap = HashMap<String, Header>()
        for (namespace in Namespace.all) {
            val namespaceRepresentation = Header.Namespace(namespace)
            val parentPath = namespace.path.substringBeforeLast('.')
            if (namespaceMap.containsKey(parentPath)) {
                namespaceRepresentation.isTopLevel = false
                (namespaceMap[parentPath]!! as Header.Namespace).add(namespaceRepresentation)
            }
            namespaceMap[namespace.path] = namespaceRepresentation
            for (symbol in namespace.symbols) {
                if (!symbol.meta.isInternal) {
                    when (symbol.kind) {
                        Symbol.Kind.Function -> {
                            symbol as CallableSymbol
                            val function = if (symbol.meta.isInline) {
                                Header.InlineFunction(symbol, program.functionBodies!![symbol]!!)
                            } else Header.Function(symbol)
                            namespaceRepresentation.add(function)
                        }
                        Symbol.Kind.StructType -> {
                            symbol as TypeSymbol.StructTypeSymbol
                            namespaceRepresentation.add(Header.Struct(symbol))
                        }
                    }
                }
            }
        }
        for (e in TypeSymbol.map) {
            val type = e.value
            if (type is TypeSymbol.StructTypeSymbol) {
                namespaceMap[e.key] = Header.Struct(type)
            }
        }
        val builder = StringBuilder()
        for (namespace in namespaceMap.values) {
            if (namespace !is Header.Namespace || (namespace.isTopLevel && namespace.isNotEmpty())) {
                builder.append(namespace.toString())
            }
        }
        return builder.toString()
    }

    interface Header {

        class Function (val symbol: CallableSymbol) : Header {
            override fun toString () = "\n[extern][cname:\"${symbol.mangledName()}\"]\nfn " + run {
                val a = ArrayList<VariableSymbol>()
                a.addAll(symbol.parameters)
                val b = if (symbol.meta.isExtension) {
                    a.removeAt(0)
                    symbol.parameters[0].type.name + '.' + symbol.name
                } else symbol.name
                b + a.joinToString(separator = ", ", prefix = " (", postfix = ")") {
                    it.name + ' ' + it.type.name
                }
            } + ' ' + symbol.returnType.name
        }

        class InlineFunction (val symbol: CallableSymbol, val body: Statement) : Header {
            override fun toString (): String {
                return "\n[inline]\nfn " + run {
                    val a = ArrayList<VariableSymbol>()
                    a.addAll(symbol.parameters)
                    val b = if (symbol.meta.isExtension) {
                        a.removeAt(0)
                        symbol.parameters[0].type.name + '.' + symbol.name
                    } else symbol.name
                    b + a.joinToString(separator = ", ", prefix = " (", postfix = ")") {
                        it.name + ' ' + it.type.name
                    }
                } + ' ' + symbol.returnType.name + " -> " + body.structureString()
            }
        }

        class Namespace (namespace: mango.interpreter.binding.Namespace) : Header {

            val name = namespace.path.substringAfterLast('.')
            val used = namespace.used

            var isTopLevel = true
            private val content = ArrayList<Header>()

            fun add (header: Header) = content.add(header)
            fun isNotEmpty () = content.isNotEmpty()

            override fun toString () = "namespace $name {" + (
                    used.joinToString(separator = "\n", prefix = "\n") +
                    content.joinToString(separator = "\n", prefix = "\n")
                ).replace("\n", "\n\t") +
                "\n}\n\n"
        }

        class Struct (val symbol: TypeSymbol.StructTypeSymbol) : Header {

            override fun toString () = "struct ${symbol.name} {" + symbol.fields.joinToString(separator = "\n", prefix = "\n").replace("\n", "\n\t") + "\n}\n\n"
        }
    }
}