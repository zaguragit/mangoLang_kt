package mango.compilation

import mango.binding.Binder
import mango.binding.BoundBlockStatement
import mango.binding.BoundGlobalScope
import mango.binding.ControlFlowGraph
import mango.symbols.VariableSymbol
import mango.lowering.Lowerer
import mango.syntax.parser.SyntaxTree
import java.io.File
import java.io.OutputStreamWriter
import java.nio.file.Path

class Compilation(
    val syntaxTree: SyntaxTree,
    val previous: Compilation?
) {

    val globalScope: BoundGlobalScope = Binder.bindGlobalScope(syntaxTree.root, previous?.globalScope)

    fun evaluate(variables: HashMap<VariableSymbol, Any?>): EvaluationResult {

        val errors = syntaxTree.errors.apply { append(globalScope.diagnostics) }
        if (errors.hasErrors()) {
            errors.sortBySpan()
            return EvaluationResult(errors.list, errors.nonErrorList)
        }

        val program = Binder.bindProgram(globalScope)

        val cfgStatement = if (!program.statement.statements.any() && program.functionBodies.any()) {
            program.functionBodies.values.last()
        } else {
            program.statement
        }
        if (cfgStatement is BoundBlockStatement) {
            val cfg = ControlFlowGraph.create(cfgStatement)
            cfg.print()
        }

        if (program.diagnostics.hasErrors()) {
            val d = program.diagnostics.apply { sortBySpan() }
            return EvaluationResult(d.list, d.nonErrorList)
        }

        val evaluator = Evaluator(program.functionBodies, program.statement, variables)
        evaluator.evaluate()
        errors.sortBySpan()
        return EvaluationResult(errors.list, errors.nonErrorList)
    }

    fun printTree() {
        val program = Binder.bindProgram(globalScope)
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