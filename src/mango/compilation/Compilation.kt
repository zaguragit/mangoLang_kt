package mango.compilation

import mango.compilation.headers.HeaderEmitter
import mango.compilation.llvm.LLVMEmitter
import mango.console.Console
import mango.interpreter.binding.Binder
import mango.interpreter.binding.BoundGlobalScope
import mango.interpreter.binding.BoundProgram
import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.symbols.VariableSymbol
import mango.interpreter.syntax.SyntaxTree
import mango.isProject
import mango.isRepl
import mango.isSharedLib
import mango.useStd
import java.io.File
import java.io.FileNotFoundException


class Compilation(
    val previous: Compilation?,
    val syntaxTrees: Collection<SyntaxTree>
) {

    val globalScope: BoundGlobalScope by lazy {
        Binder.bindGlobalScope(previous?.globalScope, syntaxTrees)
    }

    private fun getProgram(): BoundProgram = Binder.bindProgram(previous?.getProgram(), globalScope)

    fun evaluate(): CompilationResult {

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

        diagnostics.sortBySpan()
        //if (isRepl) {
        //}
        return CompilationResult(diagnostics.errorList, diagnostics.nonErrorList)
    }

    fun printTree() = printTree(globalScope.mainFn)

    fun printTree(symbol: FunctionSymbol) {
        val program = getProgram()
        symbol.printStructure()
        print(' ')
        val body = program.functions[symbol]
        body?.run { print(structureString()) }
        println()
    }

    fun emit(moduleName: String, outputPath: String, target: String, emissionType: EmissionType) {
        val program = getProgram()
        val code = LLVMEmitter.emit(program, moduleName)
        val outFile = File(outputPath)
        if (emissionType == EmissionType.IR) {
            return try {
                outFile.writeText(code)
            } catch (e: FileNotFoundException) {
                println(Console.RED + "Couldn't write to file $outputPath")
                return
            }
        }
        val llFile = File.createTempFile("mangoLang", ".ll").apply {
            deleteOnExit()
            writeText(code)
        }
        when (emissionType) {
            EmissionType.Assembly -> {
                outFile.parentFile.mkdirs()
                ProcessBuilder("llc", llFile.absolutePath, "-o=$outputPath", "-filetype=asm", "-relocation-model=pic").run {
                    inheritIO()
                    start().waitFor()
                }
            }
            EmissionType.Object -> {
                outFile.parentFile.mkdirs()
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
                outFile.parentFile.mkdirs()
                if (isSharedLib) {
                    ProcessBuilder("gcc", objFile.absolutePath, "-o", outputPath, "-shared")
                    File(if (isProject) "out/headers.m" else outputPath.substringBeforeLast('/') + "/headers.m").run {
                        createNewFile()
                        writeText(HeaderEmitter.emit(program, moduleName))
                    }
                } else if (useStd) {
                    ProcessBuilder("gcc", objFile.absolutePath, "/usr/local/lib/mangoLang/std.so", "-o", outputPath)
                } else {
                    ProcessBuilder("gcc", objFile.absolutePath, "-o", outputPath)
                }.run {
                    inheritIO()
                    start().waitFor()
                }
            }
        }
    }
}