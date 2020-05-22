package mango.syntax.lex

import mango.syntax.SyntaxType

object Translator {
    fun stringToTokenKind(string: String) = when (string) {

        "false" -> SyntaxType.False
        "true" -> SyntaxType.True

        "val" -> SyntaxType.Val
        "var" -> SyntaxType.Var

        "if" -> SyntaxType.If
        "else" -> SyntaxType.Else

        "while" -> SyntaxType.While
        "for" -> SyntaxType.For

        "break" -> SyntaxType.Break
        "continue" -> SyntaxType.Continue

        "in" -> SyntaxType.In

        "fn" -> SyntaxType.Fn

        else -> SyntaxType.Identifier
    }
}