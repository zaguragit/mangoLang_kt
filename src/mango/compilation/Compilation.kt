package mango.compilation

import mango.compilation.llvm.LLVMEmitter
import mango.interpreter.binding.Binder
import mango.interpreter.binding.BoundGlobalScope
import mango.interpreter.binding.BoundProgram
import mango.interpreter.eval.EvaluationResult
import mango.interpreter.eval.Evaluator
import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.symbols.VariableSymbol
import mango.interpreter.syntax.parser.SyntaxTree
import mango.isRepl
import java.io.File


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

        val diagnostics = syntaxTree.diagnostics
        if (diagnostics.hasErrors()) {
            diagnostics.sortBySpan()
            return EvaluationResult(null, diagnostics.errorList, diagnostics.nonErrorList)
        }
        diagnostics.append(globalScope.diagnostics)
        if (globalScope.diagnostics.hasErrors()) {
            diagnostics.sortBySpan()
            return EvaluationResult(null, diagnostics.errorList, diagnostics.nonErrorList)
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
        diagnostics.append(program.diagnostics)
        if (program.diagnostics.hasErrors()) {
            val d = program.diagnostics.apply { sortBySpan() }
            return EvaluationResult(null, d.errorList, d.nonErrorList)
        }

        val evaluator = Evaluator(program, variables)
        diagnostics.sortBySpan()
        if (isRepl) {
            val value = evaluator.evaluate()
            return EvaluationResult(value, diagnostics.errorList, diagnostics.nonErrorList)
        }
        return EvaluationResult(null, diagnostics.errorList, diagnostics.nonErrorList)
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

    fun emit(moduleName: String, references: Array<String>, outputPath: String, target: String, emissionType: EmissionType) {
        val program = getProgram()
        val code = LLVMEmitter.emit(program, moduleName, references, outputPath)
        if (emissionType == EmissionType.IR) {
            File(outputPath).writeText(code)
            return
        }
        val llFile = File.createTempFile("mangoLang", ".ll").apply {
            deleteOnExit()
            writeText(code)
        }
        if (emissionType == EmissionType.Assembly) {
            ProcessBuilder("llc", llFile.absolutePath, "-o=$outputPath", "-filetype=asm", "-relocation-model=pic").run {
                inheritIO()
                start().waitFor()
            }
            return
        }
        if (emissionType == EmissionType.Object) {
            ProcessBuilder("llc", llFile.absolutePath, "-o=$outputPath", "-filetype=obj", "-relocation-model=pic").run {
                inheritIO()
                start().waitFor()
            }
            return
        }
        val objFile = File.createTempFile("mangoLang", ".o").apply {
            deleteOnExit()
            ProcessBuilder("llc", llFile.absolutePath, "-o=$absolutePath", "-filetype=obj", "-relocation-model=pic").run {
                inheritIO()
                start().waitFor()
            }
        }
        ProcessBuilder("gcc", objFile.absolutePath, "-o", outputPath).run {
            inheritIO()
            start().waitFor()
        }
    }
}