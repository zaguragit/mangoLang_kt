package mango.interpreter.binding

import mango.interpreter.symbols.CallableSymbol
import mango.interpreter.symbols.Symbol

open class BoundScope(
    val parent: BoundScope?
) {

    val namespace: BoundNamespace? get() =
        if (parent is BoundNamespace) parent
        else parent?.namespace

    protected open val map by lazy { HashMap<Pair<String, String?>, Symbol>() }
    open val symbols: Collection<Symbol> get() = map.values

    val used = ArrayList<BoundUse>()

    fun tryDeclare(symbol: Symbol) = tryDeclare(symbol, symbol.name)
    fun tryDeclare(symbol: Symbol, name: String): Boolean {
        val extra = if (symbol is CallableSymbol) symbol.suffix else null
        if (map.containsKey(name to extra)) {
            return false
        }
        map[name to extra] = symbol
        return true
    }
    fun tryLookup (path: List<String>, extra: String? = null): Symbol? = tryLookup(path, extra, true)
    protected fun tryLookup (path: List<String>, extra: String? = null, isReal: Boolean): Symbol? {
        val parentNamespace = namespace
        return when {
            path.size == 1 -> {
                val name = path.elementAt(0)
                when {
                    map.containsKey(name to extra) -> {
                        map[name to extra]?.apply { useCounter++ }
                    }
                    parent == null -> return null
                    else -> {
                        val parentLookup = parent.tryLookup(path, extra, isReal)
                        if (parentLookup != null) {
                            parentLookup.useCounter++
                            return parentLookup
                        }
                        if (isReal) for (use in used) {
                            if (use.isInclude) {
                                val namespace = BoundNamespace[use.path]!!
                                val result = namespace.tryLookup(path, extra, false)
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
            else -> {
                if (parentNamespace != null) {
                    val parentLookup = parentNamespace.tryLookup(path, extra, false)
                    if (parentLookup != null) {
                        parentLookup.useCounter++
                        return parentLookup
                    }
                    val namespace = BoundNamespace[parentNamespace.path + '.' + path.first()]
                    if (namespace != null) {
                        val symbol = namespace.tryLookup(path.subList(1, path.size), extra, false)
                        if (symbol != null) {
                            symbol.useCounter++
                            return symbol
                        }
                    }
                }
                if (isReal) for (use in used) {
                    if (use.isInclude) {
                        val namespace = BoundNamespace[use.path + '.' + path.first()]
                        if (namespace != null) {
                            val result = namespace.tryLookup(path.toMutableList().apply { removeAt(0) }, extra, false)
                            if (result != null) {
                                result.useCounter++
                                return result
                            }
                        }
                    } else {
                        val namespace = BoundNamespace[use.path]!!
                        if (namespace.path.split('.').last() == path.first()) {
                            val result = namespace.tryLookup(path.toMutableList().apply { removeAt(0) }, extra, false)
                            if (result != null) {
                                result.useCounter++
                                return result
                            }
                        }
                    }
                }
                val namespace = BoundNamespace[path.joinToString(".").substringBeforeLast('.')]
                if (isReal && namespace != null) {
                    val lookup = namespace.tryLookup(listOf(path.last()), extra, false)
                    if (lookup != null) {
                        lookup.useCounter++
                        return lookup
                    }
                }

                if (parent != null) {
                    return parent.tryLookup(path, extra, isReal)
                }
                return null
            }
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
    fun tryLookupFunction(path: Collection<String>, isFromUse: Boolean): Pair<FunctionSymbol?, Boolean> {
        return when {
            path.size == 1 -> {
                val name = path.elementAt(0)
                when {
                    map.containsKey(name) -> {
                        val symbol = map[name]
                        if (symbol is FunctionSymbol) { symbol.apply { useCounter++ } to true } else null to false
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

    fun use(use: BoundUse) {
        used.add(use)
    }
}

