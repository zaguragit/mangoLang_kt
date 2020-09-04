package mango.compilation

import mango.console.Console
import mango.interpreter.binding.Namespace
import mango.interpreter.binding.nodes.BiOperator
import mango.interpreter.binding.nodes.UnOperator
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.text.TextLocation

class Diagnostic(
    val location: TextLocation?,
    val message: String,
    val diagnosticType: Type = Type.Error
) {
    override fun toString(): String = message

    enum class Type {
        Error,
        Warning,
        Style
    }

    fun printAsError() {
        if (location == null) {
            print(Console.RED + "error: $message" + Console.RESET)
            return
        }
        val span = location.span
        val charI = location.startCharI
        val startLine = location.text.lines[location.startLineI]
        val endLine = location.text.lines[location.endLineI]

        val spanStart = span.start
        val spanEnd = span.end

        print(Console.RED + "${location.text.fileName}[" + Console.BLUE_BRIGHT + "${location.startLineI}, $charI" + Console.RED + "]: $message" + Console.RESET + " {\n\t")
        print(location.text.getTextRange(startLine.start, spanStart))
        print(Console.RED_BOLD_BRIGHT)
        print(location.text.getTextRange(spanStart, spanEnd).replace("\n", "\n\t"))
        print(Console.RESET)
        print(location.text.getTextRange(spanEnd, endLine.end))
        println()
        println('}')
    }

    fun printAsSuggestion() {
        val span = location!!.span
        val charI = location.startCharI
        val startLine = location.text.lines[location.startLineI]
        val endLine = location.text.lines[location.endLineI]

        val spanStart = span.start
        val spanEnd = span.end

        val color = when (diagnosticType) {
            Type.Warning -> Console.YELLOW_BRIGHT
            else -> Console.CYAN
        }

        print(color + "${location.text.fileName}[" + Console.BLUE_BRIGHT + "${location.startLineI}, $charI" + color + "]: $message" + Console.RESET + " {\n\t")
        print(location.text.getTextRange(startLine.start, spanStart))
        when (diagnosticType) {
            Type.Warning -> print(Console.YELLOW_BOLD_BRIGHT)
            else -> print(Console.CYAN_BOLD_BRIGHT)
        }
        print(location.text.getTextRange(spanStart, spanEnd).replace("\n", "\n\t"))
        print(Console.RESET)
        print(location.text.getTextRange(spanEnd, endLine.end))
        println()
        println('}')
    }
}

class DiagnosticList {

    fun sortBySpan() {
        val comparator = Comparator { d0: Diagnostic, d1 ->
            if (d0.location == null || d1.location == null) {
                return@Comparator 0
            }
            var cmp = d0.location.span.start - d1.location.span.start
            if (cmp == 0) {
                cmp = d0.location.span.length - d1.location.span.length
            }
            cmp
        }
        errors.sortWith(comparator)
        nonErrors.sortWith(comparator)
    }

    private val errors = ArrayList<Diagnostic>()
    private val nonErrors = ArrayList<Diagnostic>()

    val errorList: List<Diagnostic> get() = errors
    val nonErrorList: List<Diagnostic> get() = nonErrors

    fun append(other: DiagnosticList) {
        errors.addAll(other.errors)
        nonErrors.addAll(other.nonErrors)
    }
    fun hasErrors() = errors.any()

    fun clear() {
        errors.clear()
        nonErrors.clear()
    }


    /// STYLE //////////////////////////////////////////////////////////////////////////////////////////////////////////

    private inline fun style(
        location: TextLocation,
        message: String
    ) = nonErrors.add(Diagnostic(location, message, Diagnostic.Type.Style))

    fun styleElseIfStatement(
        location: TextLocation
    ) = style(location, "Unnecessary brackets (Use \"else if {}\" instead of \"else { if {} }\")")


    /// WARNINGS ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private inline fun warn(
        location: TextLocation,
        message: String
    ) = nonErrors.add(Diagnostic(location, message, Diagnostic.Type.Warning))


    /// ERRORS /////////////////////////////////////////////////////////////////////////////////////////////////////////

    private inline fun report(
        location: TextLocation,
        message: String
    ) = errors.add(Diagnostic(location, message, Diagnostic.Type.Error))

    fun reportWrongType(
        location: TextLocation,
        value: Any?,
        expectedType: TypeSymbol
    ) = report(location, "$value isn't of type $expectedType")

    fun reportWrongType(
        location: TextLocation,
        presentType: TypeSymbol,
        expectedType: TypeSymbol
    ) = report(location, "Wrong type (found $presentType, expected $expectedType)")

    fun reportUnexpectedToken(
        location: TextLocation,
        tokenType: SyntaxType,
        expectedType: SyntaxType
    ) = report(location, "Unexpected token <$tokenType>, expected <$expectedType>")

    fun reportUnaryOperator(
        location: TextLocation,
        operatorType: SyntaxType,
        operandType: TypeSymbol
    ) = report(location, "\"${UnOperator.getString(operatorType)}\" isn't compatible with $operandType")

    fun reportBinaryOperator(
        location: TextLocation,
        leftType: TypeSymbol,
        operatorType: SyntaxType,
        rightType: TypeSymbol
    ) = report(location, "\"${BiOperator.getString(operatorType)}\" isn't compatible with $leftType and $rightType")

    fun reportUndefinedName(
        location: TextLocation,
        name: String
    ) = report(location, "Undefined name \"$name\"")

    fun reportNotFoundInNamespace(
        location: TextLocation,
        name: String,
        namespace: Namespace
    ) = report(location, "There's no \"$name\" in ${namespace.path}")

    fun reportUndefinedFunction(
        location: TextLocation,
        name: String,
        parameters: List<TypeSymbol>,
        isExtension: Boolean
    ) = report(location, "Undefined name ( ${if (isExtension) {
        parameters.elementAt(0).name + ".$name" + "(${parameters.subList(1, parameters.size) .joinToString(", ") { it.name }})"
    } else {
        name + "(${parameters.joinToString(", ") { it.name }})"
    }} )")

    fun reportSymbolAlreadyDeclared(
        location: TextLocation,
        name: String
    ) = report(location, "\"$name\" is already declared")

    fun reportFunctionAlreadyDeclared(
        location: TextLocation,
        name: String,
        parameters: List<TypeSymbol>,
        isExtension: Boolean
    ) = report(location, "${if (isExtension) {
        parameters.elementAt(0).name + ".$name" + "(${parameters.subList(1, parameters.size) .joinToString(", ") { it.name }})"
    } else {
        name + "(${parameters.joinToString(", ") { it.name }})"
    }} is already declared")

    fun reportParamAlreadyExists(
        location: TextLocation,
        name: String
    ) = report(location, "Param \"$name\" already exists")

    fun reportVarIsImmutable(
        location: TextLocation,
        name: String
    ) = report(location, "\"$name\" is immutable")

    fun reportVarIsConstant(
        location: TextLocation,
        name: String
    ) = report(location, "\"$name\" is constant")

    fun reportInvalidCharacterEscape(
        location: TextLocation,
        string: String
    ) = report(location, "The character escape \\$string doesn't exist")

    fun reportUnterminatedString(
        location: TextLocation
    ) = report(location, "Unterminated string")

    fun reportWrongArgumentCount(
        location: TextLocation,
        count: Int,
        correctCount: Int
    ) = report(location, "Wrong argument count (found $count, expected $correctCount)")

    fun reportWrongArgumentType(
        location: TextLocation,
        paramName: String,
        paramType: TypeSymbol,
        expectedType: TypeSymbol
    ) = report(location, "Argument \"${paramName}\" is of type $paramType, but $expectedType was expected")

    fun reportExpressionMustHaveValue(
        location: TextLocation
    ) = report(location, "Expression must have a value")

    fun reportUndefinedType(
        location: TextLocation,
        name: String
    ) = report(location, "Type \"$name\" doesn't exist")

    fun reportBadCharacter(
        location: TextLocation,
        char: Char
    ) = report(location, "Invalid character '$char'")

    fun reportCantCast(
        location: TextLocation,
        from: TypeSymbol,
        to: TypeSymbol
    ) = report(location, "Can't cast from type $from to type $to")

    fun reportBreakContinueOutsideLoop(
        location: TextLocation,
        keyword: String
    ) = report(location, "\"$keyword\" can't be outside a loop")

    fun reportCantReturnInUnitFunction(
        location: TextLocation
    ) = report(location, "Can't return expressions in Unit functions")

    fun reportCantReturnWithoutValue(
        location: TextLocation
    ) = report(location, "Can't use empty return statements in typed functions")

    fun reportReturnOutsideFunction(
        location: TextLocation
    ) = report(location, "Can't have return statements outside functions")

    fun reportAllPathsMustReturn(
        location: TextLocation
    ) = report(location, "Not all paths return a value")

    fun reportInvalidExpressionStatement(
        location: TextLocation
    ) = report(location, "Only assignment and call expressions can be used as statements")

    fun reportNoMainFn() = errors.add(Diagnostic(null, "No entry function found", Diagnostic.Type.Error))

    fun reportStatementCantBeGlobal(
        location: TextLocation,
        syntaxType: SyntaxType
    ) = report(location, "Only variables, functions and use statements can be global (this was <$syntaxType>)")

    fun reportDeclarationAndNameOnSameLine(
        location: TextLocation
    ) = report(location, "The declaration keyword and name should be on the same line")

    fun reportInvalidAnnotation(
        location: TextLocation
    ) = report(location, "Invalid annotation")

    fun reportMultipleEntryFuncs(
        location: TextLocation
    ) = report(location, "Can't have multiple entry functions")

    fun reportIncorrectUseStatement(
        location: TextLocation
    ) = report(location, "Incorrect use statement")

    fun reportUnterminatedMultilineComment(
        location: TextLocation
    ) = report(location, "Unterminated multiline comment")

    fun reportCantBeAfterDot(
        location: TextLocation
    ) = report(location, "Only name expressions and function calls can follow a dot")

    fun reportNotCallable(
        location: TextLocation,
        type: TypeSymbol
    ) = report(location, "Expressions of type ${type.name} can't be called")

    fun reportPointerOperationsAreUnsafe(
        location: TextLocation
    ) = report(location, "Pointer operations are unsafe (need to be performed inside an unsafe block)")

    fun reportReferenceRequiresMutableVar(
        location: TextLocation
    ) = report(location, "You can only get a reference from a mutable variable")

    /// CONF ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    fun reportConfError(
        location: TextLocation,
        lineI: Int
    ) = report(location, "Conf syntax error on line $lineI")

    fun reportConfTokenNotColon(
        location: TextLocation
    ) = report(location, "Token should be ':'")

    fun reportConfMissingMandatoryField(
        name: String
    ) = errors.add(Diagnostic(null, "Conf file is missing the \"$name\" field", Diagnostic.Type.Error))
}