package mango.compiler

import mango.parser.Parser
import mango.parser.TextFile
import mango.parser.nodes.NamespaceNode
import shared.text.SourceText
import shared.utils.DiagnosticList

class SyntaxTree internal constructor(sourceText: SourceText, projectPath: String) : TextFile(sourceText, projectPath) {

    val root: NamespaceNode
    val diagnostics: DiagnosticList

    init {
        val parser = Parser(this)
        root = parser.parseCompilationUnit()
        diagnostics = parser.diagnostics
    }

    companion object {
        fun parse(sourceText: SourceText, projectPath: String): SyntaxTree =
            SyntaxTree(sourceText, projectPath)
    }
}