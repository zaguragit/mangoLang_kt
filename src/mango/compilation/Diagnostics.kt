package mango.compilation

import mango.symbols.TypeSymbol
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

    fun sortBySpan() {
        arrayList.sortWith(Comparator { d0, d1 ->
            var cmp = d0.span.start - d1.span.start
            if (cmp == 0) {
                cmp = d0.span.length - d1.span.length
            }
            cmp
        })
    }

    private val arrayList = ArrayList<Diagnostic>()

    val list: List<Diagnostic> get() = arrayList

    fun append(other: DiagnosticList) = arrayList.addAll(other.arrayList)
    fun any() = arrayList.any()

    fun report(
        span: TextSpan,
        message: String,
        diagnosticType: Diagnostic.Type = Diagnostic.Type.Error
    ) = arrayList.add(Diagnostic(span, message, diagnosticType))

    inline fun reportWrongType(
        span: TextSpan,
        value: Any?,
        expectedType: TypeSymbol
    ) = report(span, "$value isn't of type $expectedType", Diagnostic.Type.Error)

    inline fun reportUnexpectedToken(
        span: TextSpan,
        tokenType: SyntaxType,
        expectedType: SyntaxType
    ) = report(span, "Unexpected token <$tokenType>, expected <$expectedType>")

    inline fun reportUnaryOperator(
        span: TextSpan,
        operatorType: SyntaxType,
        operandType: TypeSymbol
    ) = report(span, "$operatorType isn't compatible with $operandType")

    inline fun reportBinaryOperator(
        span: TextSpan,
        leftType: TypeSymbol,
        operatorType: SyntaxType,
        rightType: TypeSymbol
    ) = report(span, "$operatorType isn't compatible with $leftType and $rightType")

    inline fun reportUndefinedName(
        span: TextSpan,
        name: String
    ) = report(span, "Undefined name \"$name\"")

    inline fun reportSymbolAlreadyDeclared(
        span: TextSpan,
        name: String
    ) = report(span, "\"$name\" is already declared")

    inline fun reportParamAlreadyExists(
        span: TextSpan,
        name: String
    ) = report(span, "Param \"$name\" already exists")

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

    inline fun reportWrongArgumentCount(
        span: TextSpan,
        name: String,
        count: Int,
        correctCount: Int
    ) = report(span, "Wrong argument count in function \"$name\" (found $count, required $correctCount)")

    inline fun reportWrongArgumentType(
        span: TextSpan,
        paramName: String,
        paramType: TypeSymbol,
        expectedType: TypeSymbol
    ) = report(span, "Argument \"${paramName}\" is of type $paramType, but $expectedType was expected", Diagnostic.Type.Error)

    inline fun reportExpressionMustHaveValue(
        span: TextSpan
    ) = report(span, "Expression must have a value")

    inline fun reportUndefinedType(
        span: TextSpan,
        name: String
    ) = report(span, "Type \"$name\" doesn't exist")

    inline fun reportBadCharacter(
        span: TextSpan,
        char: Char
    ) = report(span, "Invalid character '$char'")

    inline fun reportCantCast(
        span: TextSpan,
        from: TypeSymbol,
        to: TypeSymbol
    ) = report(span, "Can't cast from type $from to type $to")

    inline fun reportBreakContinueOutsideLoop(
        span: TextSpan,
        keyword: String
    ) = report(span, "\"$keyword\" can't be outside a loop")
}