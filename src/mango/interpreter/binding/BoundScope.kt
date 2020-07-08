package mango.interpreter.binding

import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.symbols.Symbol
import mango.interpreter.symbols.VariableSymbol

open class BoundScope(
    val parent: BoundScope?
) {

    protected open val map by lazy { HashMap<String, Symbol>() }
    open val symbols: Collection<Symbol> get() = map.values

    val used = ArrayList<BoundUse>()

    fun tryDeclare(symbol: Symbol) = tryDeclare(symbol, symbol.name)
    fun tryDeclare(symbol: Symbol, name: String): Boolean {
        if (map.containsKey(name)) {
            return false
        }
        map[name] = symbol
        return true
    }

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
                                val namespace = BoundNamespace[use.path]
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
                    val namespace = BoundNamespace[use.path]
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
                                    val namespace = BoundNamespace[use.path]
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
                        val namespace = BoundNamespace[use.path]
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

    fun use(use: BoundUse) {
        used.add(use)
    }
}

