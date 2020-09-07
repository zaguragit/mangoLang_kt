package mango.interpreter.syntax

import mango.compilation.DiagnosticList
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.text.TextLocation
import mango.interpreter.text.TextSpan

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
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' -> readNumberToken()
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
            '(' -> Token(syntaxTree, SyntaxType.OpenParentheses, position++, string = "(")
            ')' -> Token(syntaxTree, SyntaxType.ClosedParentheses, position++, string = ")")
            '{' -> Token(syntaxTree, SyntaxType.OpenBrace, position++, string = "{")
            '}' -> Token(syntaxTree, SyntaxType.ClosedBrace, position++, string = "}")
            '[' -> Token(syntaxTree, SyntaxType.OpenBracket, position++, string = "[")
            ']' -> Token(syntaxTree, SyntaxType.ClosedBracket, position++, string = "]")
            '!' -> {
                if (lookAhead() == '=') {
                    if (peek(2) == '=') {
                        Token(syntaxTree, SyntaxType.IsNotIdentityEqual, position, string = "!==").also { position += 3 }
                    } else {
                        Token(syntaxTree, SyntaxType.IsNotEqual, position, string = "!=").also { position += 2 }
                    }
                } else if (lookAhead() == '!') {
                    Token(syntaxTree, SyntaxType.DoubleBang, position, string = "!!").also { position += 2 }
                } else {
                    Token(syntaxTree, SyntaxType.Bang, position++, string = "!")
                }
            }
            '?' -> Token(syntaxTree, SyntaxType.QuestionMark, position++, string = "?")
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
            ',' -> Token(syntaxTree, SyntaxType.Comma, position++, string = ",")
            ':' -> Token(syntaxTree, SyntaxType.Colon, position++, string = ":")
            '\'' -> readChar()
            '"' -> readString()
            '\n', '\r', ';', ';' -> Token(syntaxTree, SyntaxType.LineSeparator, position++)
            '\u0000' -> Token(syntaxTree, SyntaxType.EOF, sourceText.lastIndex)
            else -> {
                diagnostics.reportBadCharacter(TextLocation(sourceText, TextSpan(position, 1)), current)
                Token(syntaxTree, SyntaxType.Bad, position++)
            }
        }
    }

    private fun readNumberToken(): Token {
        var isFloat = false
        if (current == '.') {
            if (lookAhead() == '.') {
                return Token(syntaxTree, SyntaxType.Range, position, string = "..").also { position += 2 }
            } else if (!lookAhead().isDigit()) {
                return Token(syntaxTree, SyntaxType.Dot, position++, string = ".")
            } else {
                isFloat = true
                position++
            }
        }
        val start = position
        val stringBuilder = StringBuilder()
        var isDouble = true
        var isLong = false
        if (current == '0') {
            if (lookAhead() == 'x' || lookAhead() == 'b' || lookAhead() == 's') {
                stringBuilder.append('0')
                position++
                stringBuilder.append(current)
                position++
            }
        }
        loop@ while (current.isDigit()) {
            stringBuilder.append(current)
            position++
            while (current == '_') {
                position++
            }
            if (!current.isDigit()) {
                when (current) {
                    '.' -> {
                        if (isFloat) {
                            diagnostics.reportBadCharacter(TextLocation(sourceText, TextSpan(position, 1)), current)
                        } else {
                            isFloat = true
                            stringBuilder.append('.')
                            position++
                        }
                    }
                    'f' -> {
                        isDouble = false
                        isFloat = true
                        position++
                        break@loop
                    }
                    'l' -> {
                        if (isFloat) {
                            diagnostics.reportBadCharacter(TextLocation(sourceText, TextSpan(position, 1)), current)
                        } else {
                            isLong = true
                            position++
                            break@loop
                        }
                    }
                    else -> if (current.isLetter()) {
                        diagnostics.reportBadCharacter(TextLocation(sourceText, TextSpan(position, 1)), current)
                    }
                }
            }
        }
        val textRaw = stringBuilder.toString()
        if (isFloat) {
            if (isDouble) {
                val d = textRaw.toDoubleOrNull()
                if (d != null) {
                    return Token(syntaxTree, SyntaxType.Double, start, d, textRaw)
                }
            } else {
                val f = textRaw.toFloatOrNull()
                if (f != null) {
                    return Token(syntaxTree, SyntaxType.Float, start, f, textRaw)
                }
            }
        } else {
            val (radix, text) = when {
                textRaw.startsWith("0x") -> {
                    16 to textRaw.substring(2)
                }
                textRaw.startsWith("0b") -> {
                    2 to textRaw.substring(2)
                }
                textRaw.startsWith("0s") -> {
                    6 to textRaw.substring(2)
                }
                else -> 10 to textRaw
            }
            if (!isLong) {
                val i8 = text.toByteOrNull(radix)
                if (i8 != null) {
                    return Token(syntaxTree, SyntaxType.I8, start, i8, textRaw)
                }
                val i16 = text.toShortOrNull(radix)
                if (i16 != null) {
                    return Token(syntaxTree, SyntaxType.I16, start, i16, textRaw)
                }
                val i32 = text.toIntOrNull(radix)
                if (i32 != null) {
                    return Token(syntaxTree, SyntaxType.I32, start, i32, textRaw)
                }
            }
            val i64 = text.toLongOrNull(radix)
            if (i64 != null) {
                return Token(syntaxTree, SyntaxType.I64, start, i64, textRaw)
            }
        }
        diagnostics.reportWrongType(TextLocation(sourceText, TextSpan(start, position - start)), textRaw, TypeSymbol.Int)
        return Token(syntaxTree, SyntaxType.I32, start, 1, textRaw)
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

    fun readChar(): Token {
        val start = position++
        val char = current
        position += 2
        return Token(syntaxTree, SyntaxType.Char, start, char, "'$char'")
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
        var nestedCommentDepth = 0
        val builder = StringBuilder()
        while (true) {
            if (current == '*' && lookAhead() == '/') {
                if (nestedCommentDepth == 0) {
                    position += 2
                    break
                } else {
                    nestedCommentDepth--
                    builder.append(current)
                    position++
                }
            } else if (current == '/' && lookAhead() == '*') {
                nestedCommentDepth++
                builder.append(current)
                position++
            } else if (current == '\u0000') {
                diagnostics.reportUnterminatedMultilineComment(TextLocation(sourceText, TextSpan.fromBounds(start, position)))
                break
            } else {
                builder.append(current)
                position++
            }
        }
        val text = builder.toString()
        return Token(syntaxTree, SyntaxType.MultilineComment, start, text, "/*$text*/")
    }
}