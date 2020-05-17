package mango.compilation

import mango.binding.Binder
import mango.binding.BoundGlobalScope
import mango.symbols.VariableSymbol
import mango.lowering.Lowerer
import mango.syntax.parser.SyntaxTree

class Compilation(
    val syntaxTree: SyntaxTree,
    val previous: Compilation?
) {

    val globalScope: BoundGlobalScope = Binder.bindGlobalScope(syntaxTree.root, previous?.globalScope)

    fun evaluate(variables: HashMap<VariableSymbol, Any?>): EvaluationResult {

        val errors = syntaxTree.errors.apply { append(globalScope.diagnostics) }
        if (errors.any()) {
            return EvaluationResult(errors.list, null)
        }
        val evaluator = Evaluator(getStatement(), variables)
        val value = evaluator.evaluate()
        return EvaluationResult(errors.list, value)
    }

    fun getStatement() = Lowerer.lower(globalScope.statement)

    fun printTree() {
        getStatement().printTree()
    }
}