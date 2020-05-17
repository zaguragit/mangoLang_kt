package mango.binding

import mango.compilation.Diagnostic
import mango.symbols.FunctionSymbol
import mango.symbols.VariableSymbol

class BoundScope(
    val parent: BoundScope?
) {

    private val variableMap by lazy { HashMap<String, VariableSymbol>() }
    val variables: Collection<VariableSymbol> get() = variableMap.values

    private val functionMap by lazy { HashMap<String, FunctionSymbol>() }
    val functions: Collection<FunctionSymbol> get() = functionMap.values

    fun tryDeclareVariable(variable: VariableSymbol): Boolean {
        if (variableMap.containsKey(variable.name)) {
            return false
        }
        variableMap[variable.name] = variable
        return true
    }

    fun tryLookupVariable(name: String): Pair<VariableSymbol?, Boolean> {
        return when {
            variableMap.containsKey(name) -> variableMap[name] to true
            parent == null -> return null to false
            else -> parent.tryLookupVariable(name)
        }
    }

    fun tryDeclareFunction(function: FunctionSymbol): Boolean {
        if (functionMap.containsKey(function.name)) {
            return false
        }
        functionMap[function.name] = function
        return true
    }

    fun tryLookupFunction(name: String): Pair<FunctionSymbol?, Boolean> {
        return when {
            functionMap.containsKey(name) -> functionMap[name] to true
            parent == null -> return null to false
            else -> parent.tryLookupFunction(name)
        }
    }
}

class BoundGlobalScope(
    val previous: BoundGlobalScope?,
    val diagnostics: List<Diagnostic>,
    val variables: Collection<VariableSymbol>,
    val statement: BoundStatement)