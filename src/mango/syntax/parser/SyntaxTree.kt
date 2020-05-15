package mango.syntax.parser

import mango.compilation.DiagnosticList
import mango.text.SourceText

class SyntaxTree internal constructor(
    val sourceText: SourceText
) {

    val root: FileUnit
    val errors: DiagnosticList

    init {
        val parser = Parser(sourceText)
        root = parser.parseFileUnit()
        errors = parser.diagnostics
    }

    companion object {
        fun parse(text: String) = SyntaxTree(SourceText(text))
    }
}