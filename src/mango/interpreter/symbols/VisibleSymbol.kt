package mango.interpreter.symbols

import mango.interpreter.binding.BoundNamespace

interface VisibleSymbol {
    val path: String

    val namespace get() = BoundNamespace[path.substringBeforeLast('.')]

    fun mangledName(): String {
        this as Symbol
        if (meta.cname != null) return meta.cname!!
        if (meta.isEntry) return "main"
        if (this is CallableSymbol && parameters.isNotEmpty()) {
            return path + suffix
        }
        return path
    }
}
