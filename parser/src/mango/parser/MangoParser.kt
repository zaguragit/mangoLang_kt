package mango.parser

import shared.text.SourceText

object MangoParser {

    fun parse(text: String, filePackage: String) = SyntaxTree(SourceText(text, filePackage), filePackage)
}