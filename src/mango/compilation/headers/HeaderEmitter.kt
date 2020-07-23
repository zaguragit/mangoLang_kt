package mango.compilation.headers

import mango.compilation.Emitter
import mango.interpreter.binding.Binder
import mango.interpreter.binding.BoundNamespace
import mango.interpreter.binding.BoundProgram
import mango.interpreter.binding.nodes.statements.BoundBlockStatement
import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.symbols.Symbol

object HeaderEmitter : Emitter {

    override fun emit(
        program: BoundProgram,
        moduleName: String
    ): String {
        val namespaceMap = HashMap<String, Header.Namespace>()
        for (namespace in BoundNamespace.namespaces.values) {
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
                        symbol as FunctionSymbol
                        if (!symbol.meta.isInternal) {
                            if (symbol.meta.isInline) {
                                val function = Header.InlineableFunction(symbol, program.functionBodies!![symbol]!!)
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

        class Function (val symbol: FunctionSymbol) : Header {
            override fun toString () = "\n[extern][cname:\"${symbol.mangledName()}\"]\n" +
                    "fn " + if (symbol.meta.isExtension) {
                symbol.parameters[0].type.name + '.' + symbol.name + " (" + symbol.parameters.joinToString(separator = ", ", postfix = ")") {
                    it.name + ' ' + it.type.name
                }.substringAfter(", ")
            } else { symbol.name + symbol.parameters.joinToString(separator = ", ", prefix = " (", postfix = ")") {
                it.name + ' ' + it.type.name
            }} + ' ' + symbol.type.name
        }

        class InlineableFunction (val symbol: FunctionSymbol, val body: BoundBlockStatement) : Header {
            override fun toString () = "\n[inline]\n" +
                    "fn " + if (symbol.meta.isExtension) {
                symbol.parameters[0].type.name + '.' + symbol.name + " (" + symbol.parameters.joinToString(separator = ", ", postfix = ")") {
                    it.name + ' ' + it.type.name
                }.substringAfter(", ")
            } else { symbol.name + symbol.parameters.joinToString(separator = ", ", prefix = " (", postfix = ")") {
                it.name + ' ' + it.type.name
            }} + ' ' + symbol.type.name + ' ' + body.structureString()
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