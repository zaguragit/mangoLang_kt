package mango.parser.conf

import shared.DiagnosticList
import shared.text.TextLocation

fun DiagnosticList.reportConfError(
    location: TextLocation,
    lineI: Int
) = report(location, "Conf syntax error on line $lineI")

fun DiagnosticList.reportConfTokenNotColon(
    location: TextLocation
) = report(location, "Token should be ':'")

fun DiagnosticList.reportConfMissingMandatoryField(
    name: String
) = report(null, "Conf file is missing the \"$name\" field")