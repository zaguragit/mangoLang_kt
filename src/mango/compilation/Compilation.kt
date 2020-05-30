package mango.compilation

import mango.interpreter.Evaluator
import mango.interpreter.binding.*
import mango.interpreter.symbols.VariableSymbol
import mango.interpreter.syntax.parser.SyntaxTree

class Compilation(
    val previous: Compilation?,
    val syntaxTree: SyntaxTree
) {

    val globalScope: BoundGlobalScope = Binder.bindGlobalScope(syntaxTree.root, previous?.globalScope)

    private fun getProgram(): BoundProgram = Binder.bindProgram(previous?.getProgram(), globalScope)

    fun evaluate(variables: HashMap<VariableSymbol, Any?>): EvaluationResult {

        val errors = syntaxTree.errors.apply { append(globalScope.diagnostics) }
        if (errors.hasErrors()) {
            errors.sortBySpan()
            return EvaluationResult(errors.list, errors.nonErrorList)
        }

        val program = getProgram()

        /*val cfgStatement = if (!program.statement.statements.any() && program.functionBodies.any()) {
            program.functionBodies.values.last()
        } else {
            program.statement
        }
        if (cfgStatement is BoundBlockStatement) {
            //val cfg = ControlFlowGraph.create(cfgStatement)
            //cfg.print()
        }*/

        if (program.diagnostics.hasErrors()) {
            val d = program.diagnostics.apply { sortBySpan() }
            return EvaluationResult(d.list, d.nonErrorList)
        }

        val evaluator = Evaluator(program, variables)
        evaluator.evaluate()
        errors.sortBySpan()
        return EvaluationResult(errors.list, errors.nonErrorList)
    }

    fun printTree() {
        val program = getProgram()
        if (program.statement.statements.any()) {
            program.statement.printStructure()
        }
        else {
            for (functionBody in program.functionBodies) {
                if (!globalScope.symbols.contains(functionBody.key)) {
                    continue
                }
                functionBody.key.printStructure()
                functionBody.value.printStructure()
            }
        }
    }
}