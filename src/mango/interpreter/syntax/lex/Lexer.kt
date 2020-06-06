package mango.interpreter.syntax.lex

import mango.compilation.DiagnosticList
import mango.interpreter.text.TextSpan
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.parser.SyntaxTree
import mango.interpreter.text.TextLocation

class Lexer(
    private val syntaxTree: SyntaxTree
) {
    private val sourceText = syntaxTree.sourceText

    private var position = 0

    val diagnostics = DiagnosticList()

    private inline val current get() = peek(0)
    private inline fun lookAhead() = peek(1)
    private inline fun peek(offset: Int): Char {
        val i = position + offset
        return if (i >= sourceText.length) '\u0000' else sourceText[i]
    }

    fun nextToken(): Token {

        while (current == ' ' || current == '\t') { position++ }

        if (current.isLetter() || current == '_') {
            return readIdentifierOrKeyword()
        }

        return when (current) {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> readNumberToken()
            '+' -> Token(syntaxTree, SyntaxType.Plus, position++, string = "+")
            '-' -> {
                if (lookAhead() == '>') {
                    Token(syntaxTree, SyntaxType.LambdaArrow, position, string = "->").also { position += 2 }
                } else {
                    Token(syntaxTree, SyntaxType.Minus, position++, string = "-")
                }
            }
            '*' -> Token(syntaxTree, SyntaxType.Star, position++, string = "*")
            '/' -> when (lookAhead()) {
                '/' -> readSingleLineComment()
                '*' -> readMultilineComment()
                else -> Token(syntaxTree, SyntaxType.Div, position++, string = "/")
            }
            '%' -> Token(syntaxTree, SyntaxType.Rem, position++, string = "%")
            '&' -> {
                if (lookAhead() == '&') {
                    Token(syntaxTree, SyntaxType.LogicAnd, position, string = "&&").also { position += 2 }
                } else {
                    Token(syntaxTree, SyntaxType.BitAnd, position++, string = "&")
                }
            }
            '|' -> {
                if (lookAhead() == '|') {
                    Token(syntaxTree, SyntaxType.LogicOr, position, string = "||").also { position += 2 }
                } else {
                    Token(syntaxTree, SyntaxType.BitOr, position++, string = "|")
                }
            }
            '(' -> Token(syntaxTree, SyntaxType.OpenRoundedBracket, position++, string = "(")
            ')' -> Token(syntaxTree, SyntaxType.ClosedRoundedBracket, position++, string = ")")
            '{' -> Token(syntaxTree, SyntaxType.OpenCurlyBracket, position++, string = "{")
            '}' -> Token(syntaxTree, SyntaxType.ClosedCurlyBracket, position++, string = "}")
            '[' -> Token(syntaxTree, SyntaxType.OpenSquareBracket, position++, string = "[")
            ']' -> Token(syntaxTree, SyntaxType.ClosedSquareBracket, position++, string = "]")
            '!' -> {
                if (lookAhead() == '=') {
                    if (peek(2) == '=') {
                        Token(syntaxTree, SyntaxType.IsNotIdentityEqual, position, string = "!==").also { position += 3 }
                    } else {
                        Token(syntaxTree, SyntaxType.IsNotEqual, position, string = "!=").also { position += 2 }
                    }
                } else {
                    Token(syntaxTree, SyntaxType.Not, position++, string = "!")
                }
            }
            '=' -> {
                if (lookAhead() == '=') {
                    if (peek(2) == '=') {
                        Token(syntaxTree, SyntaxType.IsIdentityEqual, position, string = "===").also { position += 3 }
                    } else {
                        Token(syntaxTree, SyntaxType.IsEqual, position, string = "==").also { position += 2 }
                    }
                } else {
                    Token(syntaxTree, SyntaxType.Equals, position++, string = "=")
                }
            }
            '>' -> {
                if (lookAhead() == '=') {
                    Token(syntaxTree, SyntaxType.IsEqualOrMore, position, string = ">=").also { position += 2 }
                } else {
                    Token(syntaxTree, SyntaxType.MoreThan, position++, string = ">")
                }
            }
            '<' -> {
                if (lookAhead() == '=') {
                    Token(syntaxTree, SyntaxType.IsEqualOrLess, position, string = ">=").also { position += 2 }
                } else {
                    Token(syntaxTree, SyntaxType.LessThan, position++, string = ">")
                }
            }
            '.' -> {
                if (lookAhead() == '.') {
                    Token(syntaxTree, SyntaxType.Range, position, string = "..").also { position += 2 }
                } else {
                    Token(syntaxTree, SyntaxType.Dot, position++, string = ".")
                }
            }
            ',' -> Token(syntaxTree, SyntaxType.Comma, position++, string = ",")
            ':' -> Token(syntaxTree, SyntaxType.Colon, position++, string = ":")
            '"' -> readString()
            '\n', '\r', ';', ';' -> Token(syntaxTree, SyntaxType.LineSeparator, position++)
            '\u0000' -> Token(syntaxTree, SyntaxType.EOF, sourceText.lastIndex)
            else -> {
                diagnostics.reportBadCharacter(TextLocation(sourceText, TextSpan(position, 1)), current)
                Token(syntaxTree, SyntaxType.Bad, position++)
            }
        }
    }

    fun readNumberToken(): Token {
        val start = position++
        while (current.isDigit()) {
            position++
        }
        val text = sourceText.getText(start, position - start)
        val value = text.toIntOrNull()
        if (value == null) {
            diagnostics.reportWrongType(TextLocation(sourceText, TextSpan(start, position - start)), text, TypeSymbol.int)
            return Token(syntaxTree, SyntaxType.Int, start, 1, text)
        }
        return Token(syntaxTree, SyntaxType.Int, start, value, text)
    }

    fun readIdentifierOrKeyword(): Token {
        val start = position++
        while (current.isLetterOrDigit() || current == '_') {
            position++
        }
        val text = sourceText.getText(start, position - start)
        val type = Translator.stringToTokenKind(text)
        return Token(syntaxTree, type, start, string = text)
    }

    fun readString(): Token {
        val start = ++position
        val builder = StringBuilder()
        loop@ while (current != '"') {
            when (current) {
                '\\' -> {
                    when (lookAhead()) {
                        '"' -> {
                            builder.append('"')
                            position += 2
                        }
                        'n' -> {
                            builder.append('\n')
                            position += 2
                        }
                        't' -> {
                            builder.append('\t')
                            position += 2
                        }
                        '\\' -> {
                            builder.append('\\')
                            position += 2
                        }
                        'r' -> {
                            builder.append('\r')
                            position += 2
                        }
                        else -> {
                            diagnostics.reportInvalidCharacterEscape(
                                    TextLocation(sourceText, TextSpan(position, 1)),
                                    current.toString())
                            position++
                        }
                    }
                }
                '\u0000' -> {
                    diagnostics.reportUnterminatedString(TextLocation(sourceText, TextSpan(start, builder.length)))
                    break@loop
                }
                else -> {
                    builder.append(current)
                    position++
                }
            }
        }
        val text = builder.toString()
        position++
        return Token(syntaxTree, SyntaxType.String, start - 1, text, '"' + text + '"')
    }

    private fun readSingleLineComment(): Token {
        val start = position
        position += 2
        var reading = true
        val builder = StringBuilder()
        while (reading) {
            when (current) {
                '\u0000', '\n', '\r' -> reading = false
                else -> {
                    builder.append(current)
                    position++
                }
            }
        }
        val text = builder.toString()
        return Token(syntaxTree, SyntaxType.SingleLineComment, start, text, "//$text")
    }

    private fun readMultilineComment(): Token {
        val start = position
        position += 2
        var reading = true
        val builder = StringBuilder()
        while (reading) {
            when (current) {
                '\u0000', '\n', '\r' -> reading = false
                else -> {
                    builder.append(current)
                    position++
                }
            }
        }
        val text = builder.toString()
        return Token(syntaxTree, SyntaxType.SingleLineComment, start, text, "/*$text*/")
    }
}