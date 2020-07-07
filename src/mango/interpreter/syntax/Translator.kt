package mango.interpreter.syntax

import mango.interpreter.syntax.SyntaxType

object Translator {

    fun stringToTokenKind(string: String) = when (string) {

        "false" -> SyntaxType.False
        "true" -> SyntaxType.True

        "val" -> SyntaxType.Val
        "var" -> SyntaxType.Var
        "fn" -> SyntaxType.Fn

        "if" -> SyntaxType.If
        "else" -> SyntaxType.Else

        "while" -> SyntaxType.While
        "for" -> SyntaxType.For

        "break" -> SyntaxType.Break
        "continue" -> SyntaxType.Continue
        "return" -> SyntaxType.Return

        "in" -> SyntaxType.In

        "use" -> SyntaxType.Use

        else -> SyntaxType.Identifier
    }
}