package mango.parser

import shared.DiagnosticList
import shared.text.TextLocation

fun DiagnosticList.notANumber(
    location: TextLocation,
    value: String
) = report(location, "$value isn't a number")

fun DiagnosticList.reportUnexpectedToken(
        location: TextLocation,
        tokenType: SyntaxType,
        expectedType: SyntaxType
) = report(location, "Unexpected token <$tokenType>, expected <$expectedType>")

fun DiagnosticList.reportInvalidCharacterEscape(
    location: TextLocation,
    string: String
) = report(location, "The character escape \\$string doesn't exist")

fun DiagnosticList.reportUnterminatedString(
    location: TextLocation
) = report(location, "Unterminated string")

fun DiagnosticList.reportBadCharacter(
    location: TextLocation,
    char: Char
) = report(location, "Invalid character '$char'")

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

fun DiagnosticList.reportUnterminatedMultilineComment(
    location: TextLocation
) = report(location, "Unterminated multiline comment")

fun DiagnosticList.reportCantInferType(
    location: TextLocation
) = report(location, "Can't infer type")