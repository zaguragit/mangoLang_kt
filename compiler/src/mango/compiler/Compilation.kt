package mango.compiler

import mango.compiler.binding.Binder
import mango.compiler.binding.GlobalScope
import mango.compiler.binding.Program
import mango.parser.TextFile
import shared.utils.Diagnostic

class Compilation(val textFiles: Collection<TextFile>) {

    companion object {
        fun evaluate(syntaxTrees: Collection<SyntaxTree>, isSharedLib: Boolean, requireEntryFunc: Boolean): Result {
            val globalScope: GlobalScope = Binder.bindGlobalScope(syntaxTrees, requireEntryFunc)
            val program: Program = Binder.bindProgram(globalScope, isSharedLib)

            val diagnostics = globalScope.diagnostics
            if (diagnostics.hasErrors()) {
                diagnostics.sortBySpan()
                return Result(diagnostics.errorList, diagnostics.nonErrorList, program)
            }
            diagnostics.append(globalScope.diagnostics)
            if (globalScope.diagnostics.hasErrors()) {
                diagnostics.sortBySpan()
                return Result(diagnostics.errorList, diagnostics.nonErrorList, program)
            }

            diagnostics.append(program.diagnostics)
            if (program.diagnostics.hasErrors()) {
                val d = program.diagnostics.apply { sortBySpan() }
                return Result(d.errorList, d.nonErrorList, program)
            }

            diagnostics.sortBySpan()

            return Result(diagnostics.errorList, diagnostics.nonErrorList, program)
        }
    }

    data class Result(
        val errors: Collection<Diagnostic>,
        val nonErrors: Collection<Diagnostic>,
        val program: Program
    )
}