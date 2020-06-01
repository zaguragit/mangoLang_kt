package mango.compilation

import mango.interpreter.Evaluator
import mango.interpreter.binding.*
import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.symbols.VariableSymbol
import mango.interpreter.syntax.parser.SyntaxTree

class Compilation(
    val previous: Compilation?,
    val syntaxTree: SyntaxTree
) {

    val globalScope: BoundGlobalScope by lazy {
        Binder.bindGlobalScope(syntaxTree.root, previous?.globalScope)
    }

    inline val mainFn get() = globalScope.mainFn

    private fun getProgram(): BoundProgram = Binder.bindProgram(previous?.getProgram(), globalScope)

    fun evaluate(variables: HashMap<VariableSymbol, Any?>): EvaluationResult {

        val errors = syntaxTree.diagnostics
        if (errors.hasErrors()) {
            errors.sortBySpan()
            return EvaluationResult(null, errors.errorList, errors.nonErrorList)
        }
        if (globalScope.diagnostics.hasErrors()) {
            errors.apply { append(globalScope.diagnostics) }.sortBySpan()
            return EvaluationResult(null, errors.errorList, errors.nonErrorList)
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
            return EvaluationResult(null, d.errorList, d.nonErrorList)
        }

        val evaluator = Evaluator(program, variables)
        val value = evaluator.evaluate()
        errors.sortBySpan()
        return EvaluationResult(value, errors.errorList, errors.nonErrorList)
    }

    fun printTree() {
        printTree(globalScope.mainFn)
    }

    fun printTree(symbol: FunctionSymbol) {
        val program = getProgram()
        symbol.printStructure()
        print(' ')
        val body = program.functionBodies[symbol]
        body?.printStructure()
        println()
    }
}