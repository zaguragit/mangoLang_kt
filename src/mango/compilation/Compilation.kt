package mango.compilation

import mango.compilation.llvm.LLVMEmitter
import mango.interpreter.binding.Binder
import mango.interpreter.binding.BoundGlobalScope
import mango.interpreter.binding.BoundProgram
import mango.eval.EvaluationResult
import mango.eval.Evaluator
import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.symbols.VariableSymbol
import mango.interpreter.syntax.SyntaxTree
import mango.isRepl
import java.io.File


class Compilation(
    val previous: Compilation?,
    val syntaxTrees: Collection<SyntaxTree>
) {

    val globalScope: BoundGlobalScope by lazy {
        Binder.bindGlobalScope(previous?.globalScope, syntaxTrees)
    }

    private fun getProgram(): BoundProgram = Binder.bindProgram(previous?.getProgram(), globalScope)

    fun evaluate(variables: HashMap<VariableSymbol, Any?>): CompilationResult {

        val diagnostics = globalScope.diagnostics
        if (diagnostics.hasErrors()) {
            diagnostics.sortBySpan()
            return CompilationResult(diagnostics.errorList, diagnostics.nonErrorList)
        }
        diagnostics.append(globalScope.diagnostics)
        if (globalScope.diagnostics.hasErrors()) {
            diagnostics.sortBySpan()
            return CompilationResult(diagnostics.errorList, diagnostics.nonErrorList)
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
            return CompilationResult(d.errorList, d.nonErrorList)
        }

        val evaluator = Evaluator(program, variables)
        diagnostics.sortBySpan()
        if (isRepl) {
            val value = evaluator.evaluate()
            return EvaluationResult(value, diagnostics.errorList, diagnostics.nonErrorList)
        }
        return CompilationResult(diagnostics.errorList, diagnostics.nonErrorList)
    }

    fun printTree() = printTree(globalScope.mainFn)

    fun printTree(symbol: FunctionSymbol) {
        val program = getProgram()
        symbol.printStructure()
        print(' ')
        val body = program.functions[symbol]
        body?.printStructure()
        println()
    }

    fun emit(moduleName: String, references: Array<String>, outputPath: String, target: String, emissionType: EmissionType) {
        val program = getProgram()
        val code = LLVMEmitter.emit(program, moduleName, references, outputPath)
        if (emissionType == EmissionType.IR) {
            return File(outputPath).writeText(code)
        }
        val llFile = File.createTempFile("mangoLang", ".ll").apply {
            deleteOnExit()
            writeText(code)
        }
        when (emissionType) {
            EmissionType.Assembly -> {
                File(outputPath).parentFile.mkdirs()
                ProcessBuilder("llc", llFile.absolutePath, "-o=$outputPath", "-filetype=asm", "-relocation-model=pic").run {
                    inheritIO()
                    start().waitFor()
                }
            }
            EmissionType.Object -> {
                File(outputPath).parentFile.mkdirs()
                ProcessBuilder("llc", llFile.absolutePath, "-o=$outputPath", "-filetype=obj", "-relocation-model=pic").run {
                    inheritIO()
                    start().waitFor()
                }
            }
            else -> {
                val objFile = File.createTempFile("mangoLang", ".o").apply {
                    deleteOnExit()
                    ProcessBuilder("llc", llFile.absolutePath, "-o=$absolutePath", "-filetype=obj", "-relocation-model=pic").run {
                        inheritIO()
                        start().waitFor()
                    }
                }
                File(outputPath).parentFile.mkdirs()
                ProcessBuilder("gcc", objFile.absolutePath, "/usr/local/lib/mangoLang/std.so", "-o", outputPath).run {
                    inheritIO()
                    start().waitFor()
                }
            }
        }
    }
}