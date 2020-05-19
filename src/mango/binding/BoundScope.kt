package mango.binding

import mango.compilation.Diagnostic
import mango.symbols.FunctionSymbol
import mango.symbols.Symbol
import mango.symbols.VariableSymbol

class BoundScope(
    val parent: BoundScope?
) {

    private val map by lazy { HashMap<String, Symbol>() }
    val symbols: Collection<Symbol> get() = map.values

    fun tryDeclare(symbol: Symbol): Boolean {
        if (map.containsKey(symbol.name)) {
            return false
        }
        map[symbol.name] = symbol
        return true
    }

    fun tryDeclareVariable(variable: VariableSymbol): Boolean {
        if (map.containsKey(variable.name)) {
            return false
        }
        map[variable.name] = variable
        return true
    }

    fun tryLookupVariable(name: String): Pair<VariableSymbol?, Boolean> {
        return when {
            map.containsKey(name) -> {
                val symbol = map[name]
                if (symbol is VariableSymbol) { symbol to true }
                else null to false
            }
            parent == null -> return null to false
            else -> parent.tryLookupVariable(name)
        }
    }

    fun tryDeclareFunction(function: FunctionSymbol): Boolean {
        if (map.containsKey(function.name)) {
            return false
        }
        map[function.name] = function
        return true
    }

    fun tryLookupFunction(name: String): Pair<FunctionSymbol?, Boolean> {
        return when {
            map.containsKey(name) -> {
                val symbol = map[name]
                if (symbol is FunctionSymbol) { symbol to true }
                else null to false
            }
            parent == null -> null to false
            else -> parent.tryLookupFunction(name)
        }
    }
}

class BoundGlobalScope(
    val previous: BoundGlobalScope?,
    val diagnostics: List<Diagnostic>,
    val symbols: Collection<Symbol>,
    val statement: BoundStatement)