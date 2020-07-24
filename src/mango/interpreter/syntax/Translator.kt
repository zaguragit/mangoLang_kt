package mango.interpreter.syntax

object Translator {

    fun stringToTokenKind(string: String) = when (string) {

        "false" -> SyntaxType.False
        "true" -> SyntaxType.True

        "val" -> SyntaxType.Val
        "var" -> SyntaxType.Var
        "fn" -> SyntaxType.Fn
        "namespace" -> SyntaxType.NamespaceToken

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

    fun binaryOperatorToString(operator: SyntaxType) = when (operator) {
        SyntaxType.Plus -> "plus"
        SyntaxType.Minus -> "minus"
        SyntaxType.Star -> "times"
        SyntaxType.Div -> "divide"
        SyntaxType.Rem -> "rem"
        SyntaxType.BitAnd -> "and"
        SyntaxType.BitOr -> "or"
        SyntaxType.IsEqual -> "equals"
        else -> ""
    }

    fun unaryOperatorToString(operator: SyntaxType) = when (operator) {
        SyntaxType.Plus -> "plus"
        SyntaxType.Minus -> "minus"
        SyntaxType.Not -> "not"
        else -> ""
    }
}