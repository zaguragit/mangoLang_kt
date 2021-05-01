package mango.parser

import mango.parser.nodes.TopLevelNode
import shared.DiagnosticList
import shared.text.SourceText

class SyntaxTree internal constructor(sourceText: SourceText, projectPath: String) : TextFile(sourceText, projectPath) {

    val members: Collection<TopLevelNode>
    val diagnostics: DiagnosticList

    init {
        val parser = Parser(this)
        members = parser.parseCompilationUnit()
        diagnostics = parser.diagnostics
    }
}