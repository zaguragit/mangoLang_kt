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
            return EvaluationResult(errors.apply { sortBySpan() }.list)
        }

        val program = Binder.bindProgram(globalScope)
        if (program.diagnostics.any()) {
            return EvaluationResult(program.diagnostics.apply { sortBySpan() }.list)
        }

        val evaluator = Evaluator(program.functionBodies, getStatement(), variables)
        evaluator.evaluate()
        return EvaluationResult(errors.apply { sortBySpan() }.list)
    }

    fun getStatement() = Lowerer.lower(globalScope.statement)

    fun printTree() {
        getStatement().printTree()
    }
}