package mango.interpreter.syntax.parser

import mango.compilation.DiagnosticList
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
            val text = File(fileName).readText()
            val sourceText = SourceText(text, fileName)
            return SyntaxTree(sourceText)
        }
    }
}