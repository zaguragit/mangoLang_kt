package conf

import mango.compilation.DiagnosticList
import mango.interpreter.text.SourceText
import mango.interpreter.text.TextLocation
import mango.interpreter.text.TextSpan
import java.io.File

object ConfParser {

    fun parse(file: File) = parse(file.readText(), file.name)
    fun parse(text: String, fileName: String): Pair<ConfData, DiagnosticList> {
        val sourceText = SourceText(text, fileName)
        val confData = ConfData()
        var i = 0
        var lineI = 0
        var lineStart = 0
        val stringBuilder = StringBuilder()
        val tokens = ArrayList<String>()
        val diagnostics = DiagnosticList()
        loop@ while (true) {
            if (i == text.length) {
                if (stringBuilder.isNotEmpty()) {
                    tokens.add(stringBuilder.toString())
                    stringBuilder.clear()
                }
                if (tokens.size == 3) {
                    if (tokens[1] != ":") {
                        diagnostics.reportConfTokenNotColon(TextLocation(sourceText, TextSpan(lineStart + tokens[0].length, tokens[1].length)))
                    }
                    confData.table[tokens[0]] = tokens[2]
                    tokens.clear()
                } else {
                    diagnostics.reportConfError(TextLocation(sourceText, TextSpan(i - 1, 1)), lineI)
                }
                break@loop
            }
            val char = text[i]
            when {
                char == '\n' || char == '\r' -> {
                    if (stringBuilder.isNotEmpty()) {
                        tokens.add(stringBuilder.toString())
                        stringBuilder.clear()
                    }
                    when (tokens.size) {
                        3 -> {
                            if (tokens[1] != ":") {
                                diagnostics.reportConfTokenNotColon(TextLocation(sourceText, TextSpan(lineStart + tokens[0].length, tokens[1].length)))
                            }
                            confData.table[tokens[0]] = tokens[2]
                            tokens.clear()
                        }
                        0 -> {}
                        else -> diagnostics.reportConfError(TextLocation(sourceText, TextSpan(i, 1)), lineI)
                    }
                    lineI++
                    lineStart = ++i
                }
                char.isWhitespace() -> {
                    if (stringBuilder.isNotEmpty()) {
                        tokens.add(stringBuilder.toString())
                        stringBuilder.clear()
                    }
                    i++
                }
                char == ':' -> {
                    if (stringBuilder.isNotEmpty()) {
                        tokens.add(stringBuilder.toString())
                        stringBuilder.clear()
                    }
                    tokens.add(":")
                    i++
                }
                char == '"' -> {
                    if (stringBuilder.isNotEmpty()) {
                        diagnostics.reportConfError(TextLocation(sourceText, TextSpan(i, 1)), lineI)
                    }
                    if (i == text.lastIndex) {
                        diagnostics.reportUnterminatedString(TextLocation(sourceText, TextSpan(i, 1)))
                        break@loop
                    }
                    val start = i
                    i++
                    loop1@ while (true) {
                        when {
                            text[i] == '"' -> {
                                if (stringBuilder.isNotEmpty()) {
                                    tokens.add(stringBuilder.toString())
                                    stringBuilder.clear()
                                }
                                i++
                                break@loop1
                            }
                            i == text.lastIndex -> {
                                diagnostics.reportUnterminatedString(TextLocation(sourceText, TextSpan.fromBounds(start, i + 1)))
                                break@loop1
                            }
                            else -> {
                                stringBuilder.append(text[i])
                                i++
                            }
                        }
                    }
                }
                else -> {
                    when {
                        i != text.lastIndex && char == '/' && text[i + 1] == '/' -> {
                            i += 2
                            while (text[i] != '\n' && text[i] != '\r') {
                                i++
                            }
                        }
                        (char.isLetterOrDigit() || char == '_') && (stringBuilder.isNotEmpty() || !char.isDigit()) -> {
                            stringBuilder.append(char)
                            i++
                        }
                        else -> {
                            diagnostics.reportBadCharacter(TextLocation(sourceText, TextSpan(i, 1)), char)
                            i++
                        }
                    }
                }
            }
        }
        return confData to diagnostics
    }
}