package mango.syntax.lex

import mango.binding.Type
import mango.compilation.DiagnosticList
import mango.compilation.TextSpan
import mango.syntax.SyntaxType
import mango.text.SourceText

class Lexer(private val sourceText: SourceText) {

    private var position = 0

    val diagnostics = DiagnosticList()

    private inline val char get() = peek(0)
    private inline fun lookAhead() = peek(1)
    private inline fun peek(offset: Int): Char {
        val i = position + offset
        return if (i >= sourceText.length) '\u0000' else sourceText[i]
    }

    fun nextToken(): Token {

        while (char == ' ' || char == '\t' || char == ';' || char == ';') { position++ }

        if (char.isLetter()) {
            return readIdentifierOrKeyword()
        }

        return when (char) {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> readNumberToken()
            '+' -> Token(SyntaxType.Plus, position++, string = "+")
            '-' -> Token(SyntaxType.Minus, position++, string = "-")
            '*' -> Token(SyntaxType.Mul, position++, string = "*")
            '/' -> Token(SyntaxType.Div, position++, string = "/")
            '%' -> Token(SyntaxType.Rem, position++, string = "%")
            '&' -> {
                if (lookAhead() == '&') {
                    Token(SyntaxType.LogicAnd, position, string = "&&").also { position += 2 }
                } else {
                    Token(SyntaxType.BitAnd, position++, string = "&")
                }
            }
            '|' -> {
                if (lookAhead() == '|') {
                    Token(SyntaxType.LogicOr, position, string = "||").also { position += 2 }
                } else {
                    Token(SyntaxType.BitOr, position++, string = "|")
                }
            }
            '(' -> Token(SyntaxType.OpenRoundedBracket, position++, string = "(")
            ')' -> Token(SyntaxType.ClosedRoundedBracket, position++, string = ")")
            '{' -> Token(SyntaxType.OpenCurlyBracket, position++, string = "{")
            '}' -> Token(SyntaxType.ClosedCurlyBracket, position++, string = "}")
            '[' -> Token(SyntaxType.OpenSquareBracket, position++, string = "[")
            ']' -> Token(SyntaxType.ClosedSquareBracket, position++, string = "]")
            '!' -> {
                if (lookAhead() == '=') {
                    if (peek(2) == '=') {
                        Token(SyntaxType.IsNotIdentityEqual, position, string = "!==").also { position += 3 }
                    } else {
                        Token(SyntaxType.IsNotEqual, position, string = "!=").also { position += 2 }
                    }
                } else {
                    Token(SyntaxType.Not, position++, string = "!")
                }
            }
            '=' -> {
                if (lookAhead() == '=') {
                    if (peek(2) == '=') {
                        Token(SyntaxType.IsIdentityEqual, position, string = "===").also { position += 3 }
                    } else {
                        Token(SyntaxType.IsEqual, position, string = "==").also { position += 2 }
                    }
                } else {
                    Token(SyntaxType.Equals, position++, string = "=")
                }
            }
            '>' -> {
                if (lookAhead() == '=') {
                    Token(SyntaxType.IsEqualOrMore, position, string = ">=").also { position += 2 }
                } else {
                    Token(SyntaxType.MoreThan, position++, string = ">")
                }
            }
            '<' -> {
                if (lookAhead() == '=') {
                    Token(SyntaxType.IsEqualOrLess, position, string = ">=").also { position += 2 }
                } else {
                    Token(SyntaxType.LessThan, position++, string = ">")
                }
            }
            '.' -> {
                if (lookAhead() == '.') {
                    Token(SyntaxType.Range, position, string = "..").also { position += 2 }
                } else {
                    Token(SyntaxType.Dot, position++, string = ".")
                }
            }
            '"' -> readString()
            //'\n', '\r' -> Token(SyntaxType.NewLine, position++)
            '\u0000' -> Token(SyntaxType.EOF, position++)
            else -> Token(SyntaxType.Bad, position++)
        }
    }

    fun readNumberToken(): Token {
        val start = position++
        while (char.isDigit()) {
            position++
        }
        val text = sourceText.getText(start, position - start)
        val value = text.toIntOrNull()
        if (value == null) {
            diagnostics.reportWrongType(TextSpan(start, position - start), value, Type.Int)
        }
        return Token(SyntaxType.Int, start, value, text)
    }

    fun readIdentifierOrKeyword(): Token {
        val start = position++
        while (char.isLetterOrDigit()) {
            position++
        }
        val text = sourceText.getText(start, position - start)
        val type = Translator.stringToTokenKind(text)
        return Token(type, start, string = text)
    }

    fun readString(): Token {
        val start = ++position
        val builder = StringBuilder()
        loop@ while (char != '"') {
            when (char) {
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
                                    TextSpan(position, 1),
                                    char.toString())
                            position++
                        }
                    }
                }
                '\u0000' -> {
                    diagnostics.reportUnterminatedString(TextSpan(start, builder.length))
                    break@loop
                }
                else -> {
                    builder.append(char)
                    position++
                }
            }
        }
        val text = builder.toString()
        position++
        return Token(SyntaxType.String, start, text, text)
    }
}