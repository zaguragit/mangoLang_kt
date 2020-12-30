package mango.interpreter.syntax

object Translator {

    fun stringToTokenKind(string: String) = when (string) {

        "false" -> SyntaxType.False
        "true" -> SyntaxType.True
        "null" -> SyntaxType.Null

        "val" -> SyntaxType.Val
        "var" -> SyntaxType.Var
        "namespace" -> SyntaxType.NamespaceToken
        "type" -> SyntaxType.Type

        "unsafe" -> SyntaxType.Unsafe

        "loop" -> SyntaxType.Loop

        "break" -> SyntaxType.Break
        "continue" -> SyntaxType.Continue
        "return" -> SyntaxType.Return

        "as" -> SyntaxType.As

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
        SyntaxType.Bang -> "not"
        else -> ""
    }
}