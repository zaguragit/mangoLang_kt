package mango.binding

import mango.compilation.Diagnostic

class BoundScope(val parent: BoundScope?) {

    private val variableMap = HashMap<String, VariableSymbol>()

    fun tryDeclare(variable: VariableSymbol): Boolean {
        if (variableMap.containsKey(variable.name)) {
            return false
        }
        variableMap[variable.name] = variable
        return true
    }

    fun tryLookup(name: String): Pair<VariableSymbol?, Boolean> {
        if (variableMap.containsKey(name)) {
            return variableMap[name] to true
        }
        if (parent == null)
            return null to false
        return parent.tryLookup(name)
    }

    val variables: Collection<VariableSymbol> get() = variableMap.values
}

class BoundGlobalScope(
        val previous: BoundGlobalScope?,
        val diagnostics: List<Diagnostic>,
        val variables: Collection<VariableSymbol>,
        val statement: BoundStatement
) {

}