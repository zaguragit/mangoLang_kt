package mango.interpreter.syntax.parser

import mango.ExitCodes
import mango.compilation.DiagnosticList
import mango.console.Console
import mango.interpreter.text.SourceText
import java.io.File

class SyntaxTree(
    val sourceText: SourceText
) {

    val root: CompilationUnitNode
    val diagnostics: DiagnosticList

    init {
        val parser = Parser(this)
        root = parser.parseCompilationUnit()
        diagnostics = parser.diagnostics
    }

    companion object {
        fun parse(text: String) = SyntaxTree(SourceText(text, ""))
        fun load(fileName: String): SyntaxTree {
            val file = File(fileName)
            if (!file.exists()) {
                print(Console.RED)
                println("Couldn't find file \"$fileName\"")
                ExitCodes.ERROR()
            }
            if (!file.isFile) {
                print(Console.RED)
                println("\"$fileName\" isn't a file")
                ExitCodes.ERROR()
            }
            val text = file.readText()
            val sourceText = SourceText(text, fileName)
            return SyntaxTree(sourceText)
        }
    }
}