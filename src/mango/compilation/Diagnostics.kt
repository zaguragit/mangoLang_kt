package mango.compilation

import mango.console.Console
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.parser.SyntaxTree
import kotlin.math.min

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

    fun print(syntaxTree: SyntaxTree) {
        val lineI = syntaxTree.sourceText.getLineI(span.start)
        val charI = span.start - syntaxTree.sourceText.lines[lineI].start
        val line = syntaxTree.sourceText.lines[lineI]

        val spanStart = span.start
        val spanEnd = min(span.end, line.end)

        print(Console.RED + "error(" + Console.BLUE_BRIGHT + "$lineI, $charI" + Console.RED + "): $message" + Console.RESET + " {\n\t")
        print(syntaxTree.sourceText.getTextRange(line.start, spanStart))
        print(Console.RED_BOLD_BRIGHT)
        print(syntaxTree.sourceText.getTextRange(spanStart, spanEnd))
        print(Console.RESET)
        print(syntaxTree.sourceText.getTextRange(spanEnd, line.end))
        println()
        println('}')
    }
}

class DiagnosticList {

    fun sortBySpan() {
        val comparator = Comparator { d0: Diagnostic, d1 ->
            var cmp = d0.span.start - d1.span.start
            if (cmp == 0) {
                cmp = d0.span.length - d1.span.length
            }
            cmp
        }
        arrayList.sortWith(comparator)
        nonErrors.sortWith(comparator)
    }

    private val arrayList = ArrayList<Diagnostic>()
    private val nonErrors = ArrayList<Diagnostic>()

    val list: List<Diagnostic> get() = arrayList
    val nonErrorList: List<Diagnostic> get() = nonErrors

    fun append(other: DiagnosticList) {
        arrayList.addAll(other.arrayList)
        nonErrors.addAll(other.nonErrors)
    }
    fun hasErrors() = arrayList.any()

    private inline fun style(
        span: TextSpan,
        message: String
    ) = nonErrors.add(Diagnostic(span, message, Diagnostic.Type.Style))

    fun styleElseIfStatement(
        span: TextSpan
    ) = style(span, "You can write \"else if <condition> {}\" instead of \"else { if <condition> {} }\"")

    private inline fun warn(
        span: TextSpan,
        message: String
    ) = nonErrors.add(Diagnostic(span, message, Diagnostic.Type.Warning))

    private inline fun report(
        span: TextSpan,
        message: String
    ) = arrayList.add(Diagnostic(span, message, Diagnostic.Type.Error))

    fun reportWrongType(
        span: TextSpan,
        value: Any?,
        expectedType: TypeSymbol
    ) = report(span, "$value isn't of type $expectedType")

    fun reportWrongType(
        span: TextSpan,
        presentType: TypeSymbol,
        expectedType: TypeSymbol
    ) = report(span, "Wrong type (found $presentType, expected $expectedType)")

    fun reportUnexpectedToken(
        span: TextSpan,
        tokenType: SyntaxType,
        expectedType: SyntaxType
    ) = report(span, "Unexpected token <$tokenType>, expected <$expectedType>")

    fun reportUnaryOperator(
        span: TextSpan,
        operatorType: SyntaxType,
        operandType: TypeSymbol
    ) = report(span, "$operatorType isn't compatible with $operandType")

    fun reportBinaryOperator(
        span: TextSpan,
        leftType: TypeSymbol,
        operatorType: SyntaxType,
        rightType: TypeSymbol
    ) = report(span, "$operatorType isn't compatible with $leftType and $rightType")

    fun reportUndefinedName(
        span: TextSpan,
        name: String
    ) = report(span, "Undefined name \"$name\"")

    fun reportSymbolAlreadyDeclared(
        span: TextSpan,
        name: String
    ) = report(span, "\"$name\" is already declared")

    fun reportParamAlreadyExists(
        span: TextSpan,
        name: String
    ) = report(span, "Param \"$name\" already exists")

    fun reportVarIsImmutable(
        span: TextSpan,
        name: String
    ) = report(span, "Variable \"$name\" immutable and can't be assigned to")

    fun reportInvalidCharacterEscape(
        span: TextSpan,
        string: String
    ) = report(span, "The character escape \\$string doesn't exist")

    fun reportUnterminatedString(
        span: TextSpan
    ) = report(span, "Unterminated string")

    fun reportWrongArgumentCount(
        span: TextSpan,
        name: String,
        count: Int,
        correctCount: Int
    ) = report(span, "Wrong argument count in function \"$name\" (found $count, expected $correctCount)")

    fun reportWrongArgumentType(
        span: TextSpan,
        paramName: String,
        paramType: TypeSymbol,
        expectedType: TypeSymbol
    ) = report(span, "Argument \"${paramName}\" is of type $paramType, but $expectedType was expected")

    fun reportExpressionMustHaveValue(
        span: TextSpan
    ) = report(span, "Expression must have a value")

    fun reportUndefinedType(
        span: TextSpan,
        name: String
    ) = report(span, "Type \"$name\" doesn't exist")

    fun reportBadCharacter(
        span: TextSpan,
        char: Char
    ) = report(span, "Invalid character '$char'")

    fun reportCantCast(
        span: TextSpan,
        from: TypeSymbol,
        to: TypeSymbol
    ) = report(span, "Can't cast from type $from to type $to")

    fun reportBreakContinueOutsideLoop(
        span: TextSpan,
        keyword: String
    ) = report(span, "\"$keyword\" can't be outside a loop")

    fun reportCantReturnInUnitFunction(
        span: TextSpan
    ) = report(span, "Can't return expressions in Unit functions")

    fun reportCantReturnWithoutValue(
        span: TextSpan
    ) = report(span, "Can't use empty return statements in typed functions")

    fun reportReturnOutsideFunction(
        span: TextSpan
    ) = report(span, "Can't have return statements outside functions")

    fun reportAllPathsMustReturn(
        span: TextSpan
    ) = report(span, "Not all paths return a value")
}