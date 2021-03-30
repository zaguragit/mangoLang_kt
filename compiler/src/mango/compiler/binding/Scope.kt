package mango.compiler.binding

import mango.compiler.symbols.CallableSymbol
import mango.compiler.symbols.Symbol
import mango.compiler.symbols.TypeSymbol

open class Scope(
    val parent: Scope?
) {

    val namespace: Namespace? get() =
        if (parent is Namespace) parent
        else parent?.namespace

    protected open val map by lazy { HashMap<String, HashMap<CallableSymbol.Info?, Symbol>>() }
    open val symbols: Collection<Symbol> get() = map.values.flatMap { it.values }

    val used = ArrayList<UseStatement>()

    fun tryDeclare (symbol: Symbol): Boolean {
        val name = symbol.name
        val extra = if (symbol is CallableSymbol) CallableSymbol.Info(symbol) else null
        if (map.containsKey(name)) {
            if (map[name]!!.containsKey(extra)) {
                return false
            }
        } else {
            map[name] = HashMap()
        }
        map[name]!![extra] = symbol
        return true
    }

    fun tryLookupType (path: List<String>): TypeSymbol? {
        val symbol = tryLookupInternal(path, null, true)
        if (symbol is TypeSymbol) {
            return symbol
        }
        if (path.size == 1) {
            return TypeSymbol.builtin.find { it.path == path[0] }
        }
        return null
    }
    fun tryLookup (path: List<String>, extra: CallableSymbol.Info? = null): Symbol? = tryLookupInternal(path, extra, true)
        .let { if (it is TypeSymbol) null else it }
    protected fun tryLookupInternal (path: List<String>, extra: CallableSymbol.Info? = null, isReal: Boolean): Symbol? {
        val parentNamespace = namespace
        if (path.size == 1) {
            val name = path.elementAt(0)
            when {
                map.containsKey(name) -> {
                    val overloads = map[name]
                    if (extra == null) {
                        return overloads?.get(null) ?: overloads?.values?.elementAtOrNull(0)
                    }
                    if (overloads != null) {
                        for (symbol in overloads.values) {
                            if (symbol.kind != Symbol.Kind.Type &&
                                symbol.kind != Symbol.Kind.FunctionType &&
                                symbol.kind != Symbol.Kind.StructType &&
                                extra.matches(symbol.type as TypeSymbol.Fn, symbol.meta.isExtension)) {
                                return symbol.apply { useCounter++ }
                            }
                        }
                    }
                    return null
                }
                parent == null -> return null
                else -> {
                    val parentLookup = parent.tryLookupInternal(path, extra, isReal)
                    if (parentLookup != null) {
                        parentLookup.useCounter++
                        return parentLookup
                    }
                    if (isReal) for (use in used) {
                        if (use.isInclude) {
                            val namespace = Namespace[use.path]!!
                            val result = namespace.tryLookupInternal(path, extra, false)
                            if (result != null) {
                                result.useCounter++
                                return result
                            }
                        }
                    }
                    return null
                }
            }
        }
        else {
            if (parentNamespace != null) {
                val parentLookup = parentNamespace.tryLookupInternal(path, extra, false)
                if (parentLookup != null) {
                    parentLookup.useCounter++
                    return parentLookup
                }
                val namespace = Namespace[parentNamespace.path + '.' + path.first()]
                if (namespace != null) {
                    val symbol = namespace.tryLookupInternal(path.subList(1, path.size), extra, false)
                    if (symbol != null) {
                        symbol.useCounter++
                        return symbol
                    }
                }
            }
            if (isReal) for (use in used) {
                if (use.isInclude) {
                    val namespace = Namespace[use.path + '.' + path.first()]
                    if (namespace != null) {
                        val result = namespace.tryLookupInternal(path.toMutableList().apply { removeAt(0) }, extra, false)
                        if (result != null) {
                            result.useCounter++
                            return result
                        }
                    }
                } else {
                    val namespace = Namespace[use.path]!!
                    if (namespace.path.split('.').last() == path.first()) {
                        val result = namespace.tryLookupInternal(path.toMutableList().apply { removeAt(0) }, extra, false)
                        if (result != null) {
                            result.useCounter++
                            return result
                        }
                    }
                }
            }
            val namespace = Namespace[path.joinToString(".").substringBeforeLast('.')]
            if (isReal && namespace != null) {
                val lookup = namespace.tryLookupInternal(listOf(path.last()), extra, false)
                if (lookup != null) {
                    lookup.useCounter++
                    return lookup
                }
            }

            if (parent != null) {
                return parent.tryLookupInternal(path, extra, isReal)
            }
            return null
        }
    }

    /*
    fun tryLookupVariable(path: Collection<String>): Pair<VariableSymbol?, Boolean> {
        return when {
            path.size == 1 -> {
                val name = path.elementAt(0)
                when {
                    map.containsKey(name) -> {
                        val symbol = map[name]
                        if (symbol is VariableSymbol) { symbol.apply { useCounter++ } to true } else null to false
                    }
                    parent == null -> return null to false
                    else -> {
                        val parentLookup = parent.tryLookupVariable(path)
                        if (parentLookup.second) {
                            parentLookup.first!!.useCounter++
                            return parentLookup
                        }
                        for (use in used) {
                            if (use.isInclude) {
                                val namespace = BoundNamespace[use.path]!!
                                val result = namespace.tryLookupVariable(path)
                                if (result.second) {
                                    result.first!!.useCounter++
                                    return result
                                }
                            }
                        }
                        return null to false
                    }
                }
            }
            parent == null -> return null to false
            else -> {
                val parentLookup = parent.tryLookupVariable(path)
                if (parentLookup.second) {
                    parentLookup.first!!.useCounter++
                    return parentLookup
                }
                for (use in used) {
                    val namespace = BoundNamespace[use.path]!!
                    if (use.isInclude) {
                        if (namespace.path == path.first()) {
                            val result = namespace.tryLookupVariable(path)
                            if (result.second) {
                                result.first!!.useCounter++
                                return result
                            }
                        }
                    } else {
                        val result = namespace.tryLookupVariable(path)
                        if (result.second) {
                            result.first!!.useCounter++
                            return result
                        }
                    }
                }
                return null to false
            }
        }
    }
    fun tryLookupFunction(path: Collection<String>, isFromUse: Boolean): Pair<CallableSymbol?, Boolean> {
        return when {
            path.size == 1 -> {
                val name = path.elementAt(0)
                when {
                    map.containsKey(name) -> {
                        val symbol = map[name]
                        if (symbol is CallableSymbol) { symbol.apply { useCounter++ } to true } else null to false
                    }
                    parent == null -> return null to false
                    else -> {
                        if (!isFromUse) {
                            val parentLookup = parent.tryLookupFunction(path, isFromUse)
                            if (parentLookup.second) {
                                parentLookup.first!!.useCounter++
                                return parentLookup
                            }
                            for (use in used) {
                                if (use.isInclude) {
                                    val namespace = BoundNamespace[use.path]!!
                                    val result = namespace.tryLookupFunction(path, true)
                                    if (result.second) {
                                        result.first!!.useCounter++
                                        return result
                                    }
                                }
                            }
                        }
                        return null to false
                    }
                }
            }
            parent == null -> return null to false
            else -> {
                if (!isFromUse) {
                    val parentLookup = parent.tryLookupFunction(path, isFromUse)
                    if (parentLookup.second) {
                        parentLookup.first!!.useCounter++
                        return parentLookup
                    }
                    for (use in used) {
                        val namespace = BoundNamespace[use.path]!!
                        if (use.isInclude) {
                            if (namespace.path == path.first()) {
                                val result = namespace.tryLookupFunction(path, true)
                                if (result.second) {
                                    result.first!!.useCounter++
                                    return result
                                }
                            }
                        } else {
                            val result = namespace.tryLookupFunction(path, true)
                            if (result.second) {
                                result.first!!.useCounter++
                                return result
                            }
                        }
                    }
                }
                return null to false
            }
        }
    }
*/

    fun use(use: UseStatement) {
        used.add(use)
    }
}

