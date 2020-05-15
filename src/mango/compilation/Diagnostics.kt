package mango.compilation

import mango.binding.Type
import mango.syntax.SyntaxType

class Diagnostic(
    val span: TextSpan,
    val message: String,
    val diagnosticType: Type = Type.Error
) {
    override fun toString(): String = message

    enum class Type {
        Error,
        Warning,
        Style
    }
}

class DiagnosticList {

    private val arrayList = ArrayList<Diagnostic>()

    val list: List<Diagnostic> get() = arrayList

    fun append(other: DiagnosticList) = arrayList.addAll(other.arrayList)
    fun append(other: Collection<Diagnostic>) = arrayList.addAll(other)
    fun any() = arrayList.any()

    fun report(
        span: TextSpan,
        message: String,
        diagnosticType: Diagnostic.Type = Diagnostic.Type.Error
    ) = arrayList.add(Diagnostic(span, message, diagnosticType))

    inline fun reportWrongType(
        span: TextSpan,
        value: Any?,
        expectedType: Type
    ) = report(span, "$value isn't of type $expectedType", Diagnostic.Type.Error)

    inline fun reportUnexpectedToken(
        span: TextSpan,
        tokenType: SyntaxType,
        expectedType: SyntaxType
    ) = report(span, "Unexpected token <$tokenType>, expected <$expectedType>")

    inline fun reportUnaryOperator(
        span: TextSpan,
        operatorType: SyntaxType,
        operandType: Type
    ) = report(span, "$operatorType isn't compatible with $operandType")

    inline fun reportBinaryOperator(
        span: TextSpan,
        leftType: Type,
        operatorType: SyntaxType,
        rightType: Type
    ) = report(span, "$operatorType isn't compatible with $leftType and $rightType")

    inline fun reportUndefinedName(
        span: TextSpan,
        name: String
    ) = report(span, "Undefined name \"$name\"")

    inline fun reportVarAlreadyDeclared(
        span: TextSpan,
        name: String
    ) = report(span, "Variable \"$name\" is already declared")

    inline fun reportVarIsImmutable(
            span: TextSpan,
            name: String
    ) = report(span, "Variable \"$name\" immutable and can't be assigned to")

    inline fun reportInvalidCharacterEscape(
        span: TextSpan,
        string: String
    ) = report(span, "The character escape \\$string doesn't exist")

    inline fun reportUnterminatedString(
        span: TextSpan
    ) = report(span, "Unterminated string")
}