package mango.compiler

import mango.compiler.binding.Namespace
import mango.compiler.binding.nodes.BiOperator
import mango.compiler.binding.nodes.UnOperator
import mango.compiler.symbols.Symbol
import mango.compiler.symbols.TypeSymbol
import mango.parser.SyntaxType
import shared.text.TextLocation
import shared.utils.DiagnosticList

fun DiagnosticList.reportWrongType(
    location: TextLocation,
    value: Any?,
    expectedType: TypeSymbol
) = report(location, "$value isn't of type $expectedType")

fun DiagnosticList.reportWrongType(
    location: TextLocation,
    presentType: TypeSymbol,
    expectedType: TypeSymbol
) = report(location, "Wrong type (found $presentType, expected $expectedType)")

fun DiagnosticList.reportUnexpectedToken(
        location: TextLocation,
        tokenType: SyntaxType,
        expectedType: SyntaxType
) = report(location, "Unexpected token <$tokenType>, expected <$expectedType>")

fun DiagnosticList.reportUnaryOperator(
        location: TextLocation,
        operatorType: SyntaxType,
        operandType: TypeSymbol
) = report(location, "\"${UnOperator.getString(operatorType)}\" isn't compatible with $operandType")

fun DiagnosticList.reportBinaryOperator(
        location: TextLocation,
        leftType: TypeSymbol,
        operatorType: SyntaxType,
        rightType: TypeSymbol
) = report(location, "\"${BiOperator.getString(operatorType)}\" isn't compatible with $leftType and $rightType")

fun DiagnosticList.reportUndefinedName(
    location: TextLocation,
    name: String
) = report(location, "Undefined name \"$name\"")

fun DiagnosticList.reportNotFoundInNamespace(
    location: TextLocation,
    name: String,
    namespace: Namespace
) = report(location, "There's no \"$name\" in ${namespace.path}")

fun DiagnosticList.reportUndefinedFunction(
    location: TextLocation,
    name: String,
    parameters: List<TypeSymbol>,
    isExtension: Boolean
) = report(location, "Undefined name (${if (isExtension) {
    parameters.elementAt(0).path + ".$name" + "(${parameters.subList(1, parameters.size).joinToString(", ") { it.path }})"
} else {
    name + "(${parameters.joinToString(", ") { it.path }})"
}})")

fun DiagnosticList.reportNotOperator(
    location: TextLocation,
    symbol: Symbol
) = report(location, "${if (symbol.meta.isExtension) {
    (symbol.type as TypeSymbol.Fn).args.elementAt(0).path + ".${symbol.name}" + "(${(symbol.type as TypeSymbol.Fn).args.subList(1, (symbol.type as TypeSymbol.Fn).args.size).joinToString(", ") { it.path }})"
} else {
    symbol.name + "(${(symbol.type as TypeSymbol.Fn).args.joinToString(", ") { it.path }})"
}} is missing the [operator] annotation")

fun DiagnosticList.reportNoSuchField(
    location: TextLocation,
    type: TypeSymbol.StructTypeSymbol,
    name: String
) = report(location, "The \"$type\" type doesn't have a field called \"$name\"")

fun DiagnosticList.reportNotValidField(
    location: TextLocation,
    name: String
) = report(location, "\"$name\" isn't a valid field")

fun DiagnosticList.reportSymbolAlreadyDeclared(
    location: TextLocation,
    name: String
) = report(location, "\"$name\" is already declared")

fun DiagnosticList.reportFunctionAlreadyDeclared(
    location: TextLocation,
    name: String,
    parameters: List<TypeSymbol>,
    isExtension: Boolean
) = report(location, "${if (isExtension) {
    parameters.elementAt(0).path + ".$name" + "(${parameters.subList(1, parameters.size) .joinToString(", ") { it.path }})"
} else {
    name + "(${parameters.joinToString(", ") { it.path }})"
}} is already declared")

fun DiagnosticList.reportFunctionPathAlreadyDeclared(
    location: TextLocation,
    path: String,
    otherLocation: TextLocation
) = report(location, "Function with cname \"$path\" is already declared at $otherLocation")

fun DiagnosticList.reportReservedFunctionPath(
    location: TextLocation,
    cname: String
) = report(location, "The cname \"$cname\" is reserved for internal stuff")

fun DiagnosticList.reportParamAlreadyExists(
    location: TextLocation,
    name: String
) = report(location, "Param \"$name\" already exists")

fun DiagnosticList.reportVarIsImmutable(
    location: TextLocation,
    symbol: Symbol
) = report(location, "\"${symbol.name}\" is immutable")

fun DiagnosticList.reportFieldIsImmutable(
    location: TextLocation,
    field: TypeSymbol.StructTypeSymbol.Field
) = report(location, "\"${field.name}\" is immutable")

fun DiagnosticList.reportVarIsConstant(
    location: TextLocation,
    symbol: Symbol
) = report(location, "\"${symbol.name}\" is constant")

fun DiagnosticList.reportInvalidCharacterEscape(
    location: TextLocation,
    string: String
) = report(location, "The character escape \\$string doesn't exist")

fun DiagnosticList.reportUnterminatedString(
    location: TextLocation
) = report(location, "Unterminated string")

fun DiagnosticList.reportWrongArgumentCount(
    location: TextLocation,
    count: Int,
    correctCount: Int
) = report(location, "Wrong argument count (found $count, expected $correctCount)")

fun DiagnosticList.reportWrongArgumentType(
    location: TextLocation,
    paramName: String,
    paramType: TypeSymbol,
    expectedType: TypeSymbol
) = report(location, "Argument \"${paramName}\" is of type $paramType, but $expectedType was expected")

fun DiagnosticList.reportExpressionMustHaveValue(
    location: TextLocation
) = report(location, "Expression must have a value")

fun DiagnosticList.reportUndefinedType(
    location: TextLocation,
    name: String
) = report(location, "Type \"$name\" doesn't exist")

fun DiagnosticList.reportTypeNotStruct(
    location: TextLocation,
    name: String
) = report(location, "Type \"$name\" isn't a struct")

fun DiagnosticList.reportBadCharacter(
    location: TextLocation,
    char: Char
) = report(location, "Invalid character '$char'")

fun DiagnosticList.reportCantCast(
    location: TextLocation,
    from: TypeSymbol,
    to: TypeSymbol
) = report(location, "Can't cast from type $from to type $to")

fun DiagnosticList.reportBreakContinueOutsideLoop(
    location: TextLocation,
    keyword: String
) = report(location, "\"$keyword\" can't be outside a loop")

fun DiagnosticList.reportCantReturnInUnitFunction(
    location: TextLocation
) = report(location, "Can't return expressions in Unit functions")

fun DiagnosticList.reportCantReturnWithoutValue(
    location: TextLocation
) = report(location, "Can't use empty return statements in typed functions")

fun DiagnosticList.reportReturnOutsideFunction(
    location: TextLocation
) = report(location, "Can't have return statements outside functions")

fun DiagnosticList.reportAllPathsMustReturn(
    location: TextLocation
) = report(location, "Not all paths return a value")

fun DiagnosticList.reportInvalidExpressionStatement(
    location: TextLocation
) = report(location, "Only blocks, if/else & call expressions can be used as statements")

fun DiagnosticList.reportNoMainFn() = report(null, "No entry function found")

fun DiagnosticList.reportStatementCantBeGlobal(
    location: TextLocation,
    syntaxType: SyntaxType
) = report(location, "Only variables, functions and use statements can be global (this was <$syntaxType>)")

fun DiagnosticList.reportDeclarationAndNameOnSameLine(
    location: TextLocation
) = report(location, "The declaration keyword and name should be on the same line")

fun DiagnosticList.reportInvalidAnnotation(
    location: TextLocation
) = report(location, "Invalid annotation")

fun DiagnosticList.reportMultipleEntryFuncs(
    location: TextLocation
) = report(location, "Can't have multiple entry functions")

fun DiagnosticList.reportIncorrectUseStatement(
    location: TextLocation
) = report(location, "Incorrect use statement")

fun DiagnosticList.reportUnterminatedMultilineComment(
    location: TextLocation
) = report(location, "Unterminated multiline comment")

fun DiagnosticList.reportCantBeAfterDot(
    location: TextLocation
) = report(location, "Only name expressions and function calls can follow a dot")

fun DiagnosticList.reportNotCallable(
    location: TextLocation,
    type: TypeSymbol
) = report(location, "Expressions of type ${type.path} can't be called")

fun DiagnosticList.reportPointerOperationsAreUnsafe(
    location: TextLocation
) = report(location, "Pointer operations are unsafe (need to be performed inside an unsafe block)")

fun DiagnosticList.reportReferenceRequiresMutableVar(
    location: TextLocation
) = report(location, "You can only get a reference from a mutable variable")

fun DiagnosticList.reportCantInferType(
    location: TextLocation
) = report(location, "Can't infer type")

fun DiagnosticList.reportMustNotInitField(
    location: TextLocation
) = report(location, "Type fields must not be initialized")

fun DiagnosticList.reportHasToBeInitialized(
    location: TextLocation
) = report(location, "Value must be initialized")

fun DiagnosticList.reportChickenEggWithTypes(
    location: TextLocation,
    a: TypeSymbol,
    b: TypeSymbol
) = report(location, "The compiler ran into a chicken & egg situation with types $a & $b")

fun DiagnosticList.reportFieldAlreadyDeclared(
    location: TextLocation,
    typeWithOriginalField: TypeSymbol.StructTypeSymbol
) = report(location, "The field was already declared in the ${typeWithOriginalField.path} type")

fun DiagnosticList.reportOverridesNothing(
    location: TextLocation,
    name: String
) = report(location, "\"$name\" overrides nothing")

fun DiagnosticList.reportExternFnRequireCName(
    location: TextLocation
) = report(location, "External functions require a cname annotation")