package mango.compilation.headers

import mango.compilation.Emitter
import mango.interpreter.binding.Namespace
import mango.interpreter.binding.Program
import mango.interpreter.binding.nodes.statements.BlockStatement
import mango.interpreter.symbols.CallableSymbol
import mango.interpreter.symbols.Symbol

object HeaderEmitter : Emitter {

    override fun emit(
            program: Program,
            moduleName: String
    ): String {
        val namespaceMap = HashMap<String, Header.Namespace>()
        for (namespace in Namespace.namespaces.values) {
            val namespaceRepresentation = Header.Namespace(namespace.path.substringAfterLast('.'))
            val parentPath = namespace.path.substringBeforeLast('.')
            if (namespaceMap.containsKey(parentPath)) {
                namespaceRepresentation.isTopLevel = false
                namespaceMap[parentPath]!!.add(namespaceRepresentation)
            }
            namespaceMap[namespace.path] = namespaceRepresentation
            for (symbol in namespace.symbols) {
                when (symbol.kind) {
                    //Symbol.Kind.VisibleVariable ->
                    Symbol.Kind.Function -> {
                        symbol as CallableSymbol
                        if (!symbol.meta.isInternal) {
                            if (symbol.meta.isInline) {
                                val function = Header.InlineFunction(symbol, program.functionBodies!![symbol]!!)
                                namespaceRepresentation.add(function)
                            } else {
                                val function = Header.Function(symbol)
                                namespaceRepresentation.add(function)
                            }
                        }
                    }
                }
            }
        }
        val builder = StringBuilder()
        for (namespace in namespaceMap.values) {
            if (namespace.isTopLevel && namespace.isNotEmpty()) {
                builder.append(namespace.toString())
            }
        }
        return builder.toString()
    }

    interface Header {

        class Function (val symbol: CallableSymbol) : Header {
            override fun toString () = "\n[extern][cname:\"${symbol.mangledName()}\"]\n" +
                    "fn " + if (symbol.meta.isExtension) {
                symbol.parameters[0].type.name + '.' + symbol.name + " (" + symbol.parameters.joinToString(separator = ", ", postfix = ")") {
                    it.name + ' ' + it.type.name
                }.substringAfter(", ")
            } else { symbol.name + symbol.parameters.joinToString(separator = ", ", prefix = " (", postfix = ")") {
                it.name + ' ' + it.type.name
            }} + ' ' + symbol.returnType.name
        }

        class InlineFunction (val symbol: CallableSymbol, val body: BlockStatement) : Header {
            override fun toString () = "\n[inline]\n" +
                    "fn " + if (symbol.meta.isExtension) {
                symbol.parameters[0].type.name + '.' + symbol.name + " (" + symbol.parameters.joinToString(separator = ", ", postfix = ")") {
                    it.name + ' ' + it.type.name
                }.substringAfter(", ")
            } else { symbol.name + symbol.parameters.joinToString(separator = ", ", prefix = " (", postfix = ")") {
                it.name + ' ' + it.type.name
            }} + ' ' + symbol.returnType.name + ' ' + body.structureString()
        }

        class Namespace (val name: String) : Header {

            var isTopLevel = true
            private val content = ArrayList<Header>()

            fun add (header: Header) = content.add(header)
            fun isNotEmpty () = content.isNotEmpty()

            override fun toString () = "namespace $name {" + content.joinToString(separator = "\n", prefix = "\n").replace("\n", "\n\t") + "\n}\n\n"
        }
    }
}